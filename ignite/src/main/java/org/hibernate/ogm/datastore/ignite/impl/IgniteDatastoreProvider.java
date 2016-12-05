/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteIllegalStateException;
import org.apache.ignite.IgniteState;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.CacheTypeMetadata;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.thread.IgniteThread;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.ignite.IgniteDialect;
import org.hibernate.ogm.datastore.ignite.configuration.impl.IgniteProviderConfiguration;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.ignite.query.impl.QueryHints;
import org.hibernate.ogm.datastore.ignite.query.parsing.impl.IgniteQueryParserService;
import org.hibernate.ogm.datastore.ignite.transaction.impl.IgniteTransactionManagerFactory;
import org.hibernate.ogm.datastore.ignite.util.StringHelper;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

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
	/** true - if we start node and we have to stop it */
	private boolean stopOnExit = false;
	/** Associate entity name with cache key type for entities with composite id. */
	private Map<String, String> compositeIdTypes = new HashMap<>();

	public IgniteCache<Object, BinaryObject> getEntityCache(String entityName) {
		String entityCacheName = getEntityCacheName( entityName );
		return getCache( entityCacheName, true );
	}

	public IgniteCache<Object, BinaryObject> getEntityCache(EntityKeyMetadata keyMetadata) {
		String entityCacheName = getEntityCacheName( keyMetadata.getTable() );
		return getCache( entityCacheName, true );
	}

	private <K, T> IgniteCache<K, T> getCache(String entityCacheName, boolean keepBinary) {
		IgniteCache<K, T> cache = null;
		try {
			cache = cacheManager.cache( entityCacheName );
		}
		catch (IllegalStateException ex) {
			if ( Ignition.state( gridName ) == IgniteState.STOPPED ) {
				log.stoppedIgnite();
				restart();
				cache = cacheManager.cache( entityCacheName );
			}
			else {
				throw ex;
			}

		}
		if ( cache == null ) {
			log.unknownCache( entityCacheName );
			CacheConfiguration<K, T> config = new CacheConfiguration<>();
			config.setName( entityCacheName );
			cache = cacheManager.getOrCreateCache( config );
		}
		if ( keepBinary ) {
			cache = cache.withKeepBinary();
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

	public IgniteCache<Object, BinaryObject> getAssociationCache(AssociationKeyMetadata keyMetadata) {
		return keyMetadata.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION
					? getEntityCache( keyMetadata.getEntityKeyMetadata() )
					: getEntityCache( keyMetadata.getTable() );
	}

	public IgniteCache<String, Long> getIdSourceCache(IdSourceKeyMetadata keyMetadata) {
		String idSourceCacheName = getEntityCacheName( keyMetadata.getName() );
		return getCache( idSourceCacheName, false );
	}

	public BinaryObjectBuilder createBinaryObjectBuilder(String type) {
		return cacheManager.binary().builder( type );
	}

	public BinaryObjectBuilder createBinaryObjectBuilder(BinaryObject binaryObject) {
		return cacheManager.binary().builder( binaryObject );
	}

	@Override
	public void configure(Map map) {
		config = new IgniteProviderConfiguration();
		config.initialize( map );
	}

	@Override
	public void stop() {
		if ( cacheManager != null && stopOnExit ) {
			Ignition.stop( cacheManager.name(), true );
		}
	}

	@Override
	public void start() {
		try {
			localNode = Thread.currentThread() instanceof IgniteThread; // vk: take local node instance
			if ( localNode ) {
				cacheManager = (IgniteEx) Ignition.localIgnite();
				gridName = cacheManager.name();
			}
			else {
				gridName = config.getOrCreateGridName();
				try {
					if ( gridName != null ) {
						cacheManager = (IgniteEx) Ignition.ignite( gridName );
					}
					else {
						cacheManager = (IgniteEx) Ignition.ignite();
					}
				}
				catch (IgniteIllegalStateException iise) {
					// not found, then start
					IgniteConfiguration conf = config.getOrCreateIgniteConfiguration();
					conf.setGridName( gridName );
					if ( !( jtaPlatform instanceof NoJtaPlatform ) ) {
						conf.getTransactionConfiguration().setTxManagerFactory( new IgniteTransactionManagerFactory( jtaPlatform ) );
					}
					cacheManager = (IgniteEx) Ignition.start( conf );
					stopOnExit = true;
				}
			}
		}
		catch (ServiceException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw log.unableToStartDatastoreProvider( ex );
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

	public <T> List<T> affinityCall(String cacheName, Object affinityKey, SqlFieldsQuery query) {
		ComputeForLocalQueries<T> call = new ComputeForLocalQueries<>( cacheName, query );
		return cacheManager.compute().affinityCall( cacheName, affinityKey, call );
	}

	private static class ComputeForLocalQueries<T> implements IgniteCallable<List<T>> {

		private final String cacheName;
		private final SqlFieldsQuery query;
		@IgniteInstanceResource
		private Ignite ignite;

		public ComputeForLocalQueries(String cacheName, SqlFieldsQuery query) {
			this.cacheName = cacheName;
			this.query = query.setLocal( true );
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<T> call() throws Exception {
			IgniteCache<Object, BinaryObject> cache = ignite.cache( cacheName );
			if ( cache == null ) {
				throw log.cacheNotFound( cacheName );
			}
			cache = cache.withKeepBinary();
			return (List<T>) cache.query( query ).getAll();
		}
	}

	public SqlFieldsQuery createSqlFieldsQueryWithLog(String sql, QueryHints hints, Object... args) {
		String comment = hints != null ? hints.toComment() : "";
		jdbcServices.getSqlStatementLogger().logStatement( comment + sql );

//		sql = "SELECT _KEY, _VAL FROM Game WHERE playedOn_id_countryCode='DE' AND playedOn_id_sequenceNo=123";
		SqlFieldsQuery query = new SqlFieldsQuery( sql );
		if ( args != null ) {
			query.setArgs( args );
		}

		return query;
	}

	/**
	 * Converting entity key to cache key
	 *
	 * @param key entity key
	 * @return string key
	 */
	public Object createKeyObject(EntityKey key) {
		Object result = null;
		if ( key.getColumnValues().length == 1 ) {
			result = key.getColumnValues()[0];
		}
		else {
			BinaryObjectBuilder builder = createBinaryObjectBuilder( findKeyType( key.getMetadata() ) );
			for ( int i = 0; i < key.getColumnNames().length; i++ ) {
				builder.setField( StringHelper.stringAfterPoint( key.getColumnNames()[i] ), key.getColumnValues()[i] );
			}
			result = builder.build();
		}
		return result;
	}

	public Object createAssociationKeyObject( RowKey rowKey, AssociationKeyMetadata keyMetadata ) {
		Object result = null;
		if ( IgniteAssociationSnapshot.isThirdTableAssociation( keyMetadata ) ) {
			result = UUID.randomUUID().toString();
		}
		else {
			String associationKeyColumns[] = keyMetadata.getAssociatedEntityKeyMetadata().getAssociationKeyColumns();
			if ( associationKeyColumns.length == 1 ) {
				result = rowKey.getColumnValue( associationKeyColumns[0] );
			}
			else {
				BinaryObjectBuilder builder = createBinaryObjectBuilder( findKeyType( keyMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata() ) );
				for ( int i = 0; i < associationKeyColumns.length; i++ ) {
					builder.setField( StringHelper.stringAfterPoint( associationKeyColumns[i] ), rowKey.getColumnValue( associationKeyColumns[i] ) );
				}
				result = builder.build();
			}
		}
		return result;
	}

	/**
	 * Converting association key to cache key
	 *
	 * @param key - association key
	 * @return string key
	 */
	public Object createParentKeyObject(AssociationKey key) {
		Object result = null;
		if ( key.getColumnValues().length == 1 ) {
			result = key.getColumnValues()[0];
		}
		else {
			throw new UnsupportedOperationException( "Not implemented yet" );
		}
		return result;
	}

	/**
	 * Finds key type name for cache for entities with composite id
	 * @param keyMetadata
	 * @return
	 */
	private String findKeyType(EntityKeyMetadata keyMetadata) {
		String result = compositeIdTypes.get( keyMetadata.getTable() );
		if ( result == null ) {
			String cacheType = getEntityTypeName( keyMetadata.getTable() );
			IgniteCache<Object, BinaryObject> cache = getEntityCache( keyMetadata );
			CacheConfiguration cacheConfig = cache.getConfiguration( CacheConfiguration.class );
			if ( cacheConfig.getQueryEntities() != null ) {
				for ( QueryEntity qe : (Collection<QueryEntity>) cacheConfig.getQueryEntities() ) {
					if ( qe.getValueType() != null && cacheType.equalsIgnoreCase( qe.getValueType() ) ) {
						result = qe.getKeyType();
						break;
					}
				}
			}
			if ( result == null ) {
				if ( cacheConfig.getTypeMetadata() != null ) {
					for ( CacheTypeMetadata ctm : (Collection<CacheTypeMetadata>) cacheConfig.getTypeMetadata() ) {
						if ( ctm.getValueType() != null && cacheType.equalsIgnoreCase( ctm.getValueType() ) ) {
							result = ctm.getKeyType();
							break;
						}
					}
				}
				if ( result == null ) {
					//if nothing found we use id field name
					result = StringHelper.stringBeforePoint( keyMetadata.getColumnNames()[0] );
					result = StringUtils.capitalize( result );
				}
			}
			compositeIdTypes.put( keyMetadata.getTable(), result );
		}
		return result;
	}

	/**
	 * Get the entity type from the metadata
	 *
	 * @param keyMetadata metadata
	 * @return type
	 */
	public String getEntityTypeName(String entity) {
		return StringHelper.stringAfterPoint( entity ) ;
	}

	/**
	 * Get name of cache from full entity name
	 *
	 * @param entity
	 * @return cache name
	 */
	private String getEntityCacheName(String entity) {
		return StringHelper.stringBeforePoint( entity ) ;
	}
}
