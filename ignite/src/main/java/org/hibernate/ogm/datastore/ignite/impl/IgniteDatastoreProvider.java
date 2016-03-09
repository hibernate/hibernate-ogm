/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import java.util.Map;

import org.apache.ignite.IgniteAtomicSequence;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteIllegalStateException;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.IgnitionEx;
import org.apache.ignite.internal.processors.cache.IgniteCacheProxy;
import org.apache.ignite.thread.IgniteThread;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.ignite.IgniteDialect;
import org.hibernate.ogm.datastore.ignite.configuration.impl.IgniteProviderConfiguration;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.ignite.query.parsing.impl.IgniteQueryParserService;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * Provides access to a Ignite instance
 *
 * @author Dmitriy Kozlov
 *
 */
public class IgniteDatastoreProvider extends BaseDatastoreProvider
							implements Startable, Stoppable, ServiceRegistryAwareService, Configurable {

	private static final long serialVersionUID = 2278253954737494852L;

	private static final Log log = LoggerFactory.getLogger();

	protected JtaPlatform jtaPlatform;
	protected IgniteEx cacheManager;
	protected IgniteProviderConfiguration config;

	private String gridName;
	/** true - if we run inside the server node (for distributed tasks) */
	private boolean localNode = false;

	public IgniteCache<String, BinaryObject> getEntityCache(String entityCacheName) {
		IgniteCache<String, BinaryObject> cache = cacheManager.cache( entityCacheName );
		if (cache == null) {
			CacheConfiguration<String, BinaryObject> config = new CacheConfiguration<>();
			config.setName( entityCacheName );
			cache = cacheManager.getOrCreateCache( config );
		}
		// ignite.1.5.1-b1
//		cache = ((IgniteCacheProxy<String, BinaryObject>)cache).keepPortable();
		// ignite.1.5.1.final
		cache = ((IgniteCacheProxy<String, BinaryObject>) cache).keepBinary();
		return cache;
	}

	public IgniteCache<String, BinaryObject> getEntityCache(EntityKeyMetadata keyMetaData) {
		String entityCacheName = getKeyProvider().getEntityCache( keyMetaData );
		return getEntityCache( entityCacheName );
	}

	public IgniteCache<String, BinaryObject> getAssociationCache(AssociationKeyMetadata keyMetaData) {
		String entityCacheName = getKeyProvider().getAssociationCache( keyMetaData );
		IgniteCache<String, BinaryObject> cache = cacheManager.cache( entityCacheName );
		if (cache == null) {
			CacheConfiguration<String, BinaryObject> config = new CacheConfiguration<>();
			config.setName( entityCacheName );
			cache = cacheManager.getOrCreateCache( config );
		}
		// ignite.1.5.1-b1
//		cache = ((IgniteCacheProxy<String, BinaryObject>)cache).keepPortable();
		// ignite.1.5.1.final
		cache = ((IgniteCacheProxy<String, BinaryObject>) cache).keepBinary();
		return cache;
	}

	public IgniteCache<String, Object> getIdSourceCache(IdSourceKeyMetadata keyMetaData) {
		String idSourceCacheName = getKeyProvider().getIdSourceCache( keyMetaData );
		IgniteCache<String, Object> cache = cacheManager.cache( idSourceCacheName );
		if (cache == null) {
			CacheConfiguration<String, Object> config = new CacheConfiguration<>();
			config.setName( idSourceCacheName );
			cache = cacheManager.getOrCreateCache( config );
		}
		return cache;
	}

	public BinaryObjectBuilder getBinaryObjectBuilder(String type) {
		return cacheManager.binary().builder( type );
	}

	@Override
	public void configure(Map map) {
		config = new IgniteProviderConfiguration();
		config.initialize( map );
	}

	@Override
	public void stop() {
		if (cacheManager != null && !localNode) {
			cacheManager.close();
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
}
