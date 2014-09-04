/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.impl;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.infinispan.InfinispanDialect;
import org.hibernate.ogm.datastore.infinispan.dialect.impl.AssociationKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.dialect.impl.EntityKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.dialect.impl.EntityKeyMetadataExternalizer;
import org.hibernate.ogm.datastore.infinispan.dialect.impl.IdSourceKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.dialect.impl.RowKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.impl.configuration.InfinispanConfiguration;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DefaultDatastoreNames;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;
import org.infinispan.Cache;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Provides access to Infinispan's CacheManager; one CacheManager is needed for all caches,
 * it can be taken via JNDI or started by this ServiceProvider; in this case it will also
 * be stopped when no longer needed.
 *
 * @author Sanne Grinovero
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class InfinispanDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable,
													ServiceRegistryAwareService, Configurable {

	private static final Log log = LoggerFactory.make();

	private JtaPlatform jtaPlatform;
	private JndiService jndiService;
	private Map<String,Cache> caches;
	private boolean isCacheProvided;
	private EmbeddedCacheManager cacheManager;
	private final InfinispanConfiguration config = new InfinispanConfiguration();

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return InfinispanDialect.class;
	}

	@Override
	public void start() {
		try {
			String jndiProperty = config.getJndiName();
			if ( jndiProperty == null ) {
				cacheManager = createCustomCacheManager( config.getConfigurationUrl(), jtaPlatform );
				isCacheProvided = false;
			}
			else {
				log.tracef( "Retrieving Infinispan from JNDI at %1$s", jndiProperty );
				cacheManager = (EmbeddedCacheManager) jndiService.locate( jndiProperty );
				isCacheProvided = true;
			}
		}
		catch (RuntimeException e) {
			throw log.unableToInitializeInfinispan( e );
		}
		eagerlyInitializeCaches( cacheManager );
		//clear resources
		this.jtaPlatform = null;
		this.jndiService = null;
	}

	/**
	 * Need to make sure all needed caches are started before state transfer happens.
	 * This prevents this node to return undefined cache errors during replication
	 * when other nodes join this one.
	 * @param cacheManager
	 */
	private void eagerlyInitializeCaches(EmbeddedCacheManager cacheManager) {
		caches = new ConcurrentHashMap<String, Cache>( 3 );
		putInLocalCache( cacheManager, DefaultDatastoreNames.ASSOCIATION_STORE );
		putInLocalCache( cacheManager, DefaultDatastoreNames.ENTITY_STORE );
		putInLocalCache( cacheManager, DefaultDatastoreNames.IDENTIFIER_STORE );
	}

	private void putInLocalCache(EmbeddedCacheManager cacheManager, String cacheName) {
		caches.put( cacheName, cacheManager.getCache( cacheName ) );
	}

	private EmbeddedCacheManager createCustomCacheManager(URL configUrl, JtaPlatform platform) {
		TransactionManagerLookupDelegator transactionManagerLookupDelegator = new TransactionManagerLookupDelegator( platform );
		try {
			InputStream configurationFile = configUrl.openStream();
			try {
				EmbeddedCacheManager tmpCacheManager = new DefaultCacheManager( configurationFile, false );

				AdvancedExternalizer<?> entityKeyExternalizer = EntityKeyExternalizer.INSTANCE;
				AdvancedExternalizer<?> associationKeyExternalizer = AssociationKeyExternalizer.INSTANCE;
				AdvancedExternalizer<?> rowKeyExternalizer = RowKeyExternalizer.INSTANCE;
				AdvancedExternalizer<?> entityKeyMetadataExternalizer = EntityKeyMetadataExternalizer.INSTANCE;
				AdvancedExternalizer<?> idGeneratorKeyExternalizer = IdSourceKeyExternalizer.INSTANCE;

				// override global configuration from the config file to inject externalizers
				GlobalConfiguration globalConfiguration = new GlobalConfigurationBuilder()
					.read( tmpCacheManager.getCacheManagerConfiguration() )
					.serialization()
						.addAdvancedExternalizer( entityKeyExternalizer.getId(), entityKeyExternalizer )
						.addAdvancedExternalizer( associationKeyExternalizer.getId(), associationKeyExternalizer )
						.addAdvancedExternalizer( rowKeyExternalizer.getId(), rowKeyExternalizer )
						.addAdvancedExternalizer( entityKeyMetadataExternalizer.getId(), entityKeyMetadataExternalizer )
						.addAdvancedExternalizer( idGeneratorKeyExternalizer.getId(), idGeneratorKeyExternalizer )
					.build();

				cacheManager = new DefaultCacheManager( globalConfiguration, false );

				// override the named cache configuration defined in the configuration file to
				// inject the platform TransactionManager
				for (String cacheName : tmpCacheManager.getCacheNames() ) {
					Configuration originalCfg = tmpCacheManager.getCacheConfiguration( cacheName );
					Configuration newCfg = new ConfigurationBuilder()
						.read( originalCfg )
							.transaction()
								.transactionManagerLookup( transactionManagerLookupDelegator )
						.build();
					cacheManager.defineConfiguration( cacheName, newCfg );
				}

				cacheManager.start();
				return cacheManager;
			}
			finally {
				if ( configurationFile != null ) {
					configurationFile.close();
				}
			}
		}
		catch (Exception e) {
			throw raiseConfigurationError( e, configUrl.toString() );
		}
	}

	public EmbeddedCacheManager getEmbeddedCacheManager() {
		return cacheManager;
	}

	//prefer generic form over specific ones to prepare for flexible cache setting
	public Cache getCache(String name) {
		return caches.get( name );
	}

	@Override
	public void stop() {
		if ( !isCacheProvided && cacheManager != null ) {
			cacheManager.stop();
		}
	}

	private HibernateException raiseConfigurationError(Exception e, String cfgName) {
		return new HibernateException(
				"Could not start Infinispan CacheManager using as configuration file: " + cfgName, e
		);
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		jtaPlatform = serviceRegistry.getService( JtaPlatform.class );
		jndiService = serviceRegistry.getService( JndiService.class );
	}

	@Override
	public void configure(Map configurationValues) {
		this.config.initConfiguration( configurationValues );
	}
}
