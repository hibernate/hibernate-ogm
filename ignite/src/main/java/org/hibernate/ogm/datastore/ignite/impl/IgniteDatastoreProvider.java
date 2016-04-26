/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import java.util.List;
import java.util.Map;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteIllegalStateException;
import org.apache.ignite.IgniteState;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.processors.cache.IgniteCacheProxy;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.thread.IgniteThread;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.ignite.IgniteDialect;
import org.hibernate.ogm.datastore.ignite.configuration.impl.IgniteProviderConfiguration;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.ignite.query.parsing.impl.IgniteQueryParserService;
import org.hibernate.ogm.datastore.ignite.transaction.impl.IgniteTransactionManagerFactory;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;
import org.jetbrains.annotations.Nullable;

/**
 * Provides access to a Ignite instance
 *
 * @author Dmitriy Kozlov
 */
public class IgniteDatastoreProvider extends BaseDatastoreProvider
							implements Startable, Stoppable, ServiceRegistryAwareService, Configurable {

	private static final long serialVersionUID = 2278253954737494852L;

	private static final Log log = LoggerFactory.getLogger();

	private JtaPlatform jtaPlatform;
	private JdbcServices jdbcServices;
	private IgniteEx cacheManager;
	private IgniteProviderConfiguration config;

	private String gridName;
	/** true - if we run inside the server node (for distributed tasks) */
	private boolean localNode = false;

	public IgniteCache<String, BinaryObject> getEntityCache(String entityName) {
		String entityCacheName = getKeyProvider().getEntityCache( entityName );
		return getCache( entityCacheName, true );
	}

	public IgniteCache<String, BinaryObject> getEntityCache(EntityKeyMetadata keyMetaData) {
		String entityCacheName = getKeyProvider().getEntityCache( keyMetaData );
		return getCache( entityCacheName, true );
	}

	private <T> IgniteCache<String, T> getCache(String entityCacheName, boolean keepBinary) {
		IgniteCache<String, T> cache = null;
		try {
			cache = cacheManager.cache( entityCacheName );
		}
		catch (IllegalStateException ex) {
			if (Ignition.state(gridName) == IgniteState.STOPPED) {
				log.info("Cache stopped.Trying to restart");
				restart();
				cache = cacheManager.cache( entityCacheName );
			}
			else {
				throw ex;
			}
			
		}
		if (cache == null) {
			log.warn("Unknown cache '" + entityCacheName + "'. Creating new with default settings.");
			CacheConfiguration<String, T> config = new CacheConfiguration<>();
			config.setName( entityCacheName );
			cache = cacheManager.getOrCreateCache( config );
		}
		if (keepBinary) {
			cache = ((IgniteCacheProxy<String, T>) cache).keepBinary();
		}
		return cache;
	}

	private void restart() {
		if ( cacheManager.isRestartEnabled() ) {
			Ignition.restart( false );
		} 
		else {
			start();
		}
	}

	public IgniteCache<String, BinaryObject> getAssociationCache(AssociationKeyMetadata keyMetadata) {
		String entityCacheName = getKeyProvider().getEntityCache( keyMetadata.getTable() );
		return getCache(entityCacheName, true);
	}

	public IgniteCache<String, Object> getIdSourceCache(IdSourceKeyMetadata keyMetaData) {
		String idSourceCacheName = getKeyProvider().getIdSourceCache( keyMetaData );
		return getCache(idSourceCacheName, false);
	}

	public BinaryObjectBuilder getBinaryObjectBuilder(String type) {
		return cacheManager.binary().builder( type );
	}

	public BinaryObjectBuilder getBinaryObjectBuilder(BinaryObject binaryObject) {
		return cacheManager.binary().builder( binaryObject );
	}

	@Override
	public void configure(Map map) {
		config = new IgniteProviderConfiguration();
		config.initialize( map );
	}

	@Override
	public void stop() {
		if (cacheManager != null && !localNode) {
			Ignition.stop( cacheManager.name(), true );
		}
	}

	private String createGridName() {
		String result = null;
		if (config.getUrl() != null) {
			result = config.getUrl().getPath();
			result = result.replaceAll( "[\\,\\\",:,\\*,\\/,\\\\]", "_" );
		}
		return result;
	}

	@Override
	public void start() {
		try {
			localNode = Thread.currentThread() instanceof IgniteThread; //vk: take local node instance
			if (localNode) {
				cacheManager = (IgniteEx) Ignition.localIgnite();
				gridName = cacheManager.name();
			}
			else {
				gridName = createGridName();
				try {
					if (gridName != null) {
						cacheManager = (IgniteEx) Ignition.ignite( gridName );
					}
					else {
						cacheManager = (IgniteEx) Ignition.ignite();
					}
				}
				catch (IgniteIllegalStateException iise) {
					//not found, then start
					IgniteConfiguration conf = IgnitionEx.loadConfiguration( config.getUrl() ).get1();
					conf.setGridName( gridName );
					if (!(jtaPlatform instanceof NoJtaPlatform)) {
						conf.getTransactionConfiguration().setTxManagerFactory( new IgniteTransactionManagerFactory( jtaPlatform ) );
					}
					cacheManager = (IgniteEx) Ignition.start( conf );
				}
			}
		}
		catch (Exception e) {
			throw log.unableToStartDatastoreProvider( e );
		}
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistryImplementor) {
		this.jtaPlatform = serviceRegistryImplementor.getService( JtaPlatform.class );
		this.jdbcServices = serviceRegistryImplementor.getService( JdbcServices.class );
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return IgniteCacheInitializer.class;
	}

	public IgniteKeyProvider getKeyProvider() {
		return IgniteKeyProvider.INSTANCE;
	}

	public IgniteAtomicSequence atomicSequence(String name, int initialValue, boolean create) {
		return cacheManager.atomicSequence( name, initialValue, create );
	}

	public boolean isClientMode() {
		return cacheManager.configuration().isClientMode();
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return IgniteQueryParserService.class;
	}

	public String getGridName() {
		return gridName;
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return IgniteDialect.class;
	}
	
	public <T> List<T> affinityCall( String cacheName, Object affinityKey, SqlFieldsQuery query ) {
		ComputeForLocalQueries<T> call = new ComputeForLocalQueries<>( cacheName, query );
		return cacheManager.compute().affinityCall(cacheName, affinityKey, call );
	}

	private static class ComputeForLocalQueries<T> implements IgniteCallable<List<T>> {
		
		private final String cacheName;
		private final SqlFieldsQuery query;
		@IgniteInstanceResource
		private Ignite ignite;

		public ComputeForLocalQueries( String cacheName, SqlFieldsQuery query ) {
			this.cacheName = cacheName;
			this.query = query.setLocal( true );
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<T> call() throws Exception {
			IgniteCache<String, BinaryObject> cache = ignite.cache( cacheName );
			if ( cache == null )
				throw new IgniteException( "Cache '" + cacheName + "' not found" );
			cache = ((IgniteCacheProxy<String, BinaryObject>) cache ).keepBinary();
			return (List<T>)cache.query( query ).getAll();
		}
	}

	public SqlFieldsQuery createSqlFieldsQueryWithLog(String sql, Object... args) {
		
		jdbcServices.getSqlStatementLogger().logStatement( sql );
		
		SqlFieldsQuery query = new SqlFieldsQuery(sql);
		if ( args != null ) {
			query.setArgs(args);
		}
		
		return query;
	}

}
