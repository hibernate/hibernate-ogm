/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.infinispan.impl.TransactionManagerLookupDelegator;
import org.hibernate.ogm.datastore.infinispan.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispan.logging.impl.LoggerFactory;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.SerializationConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.TransactionMode;

/**
 * Provides access to the ISPN caches used for storing entities, associations and id sources. The number of caches used
 * depends on the specific implementation of this contract.
 * <p>
 * Implementations need to make sure all needed caches are started before state transfer happens. This prevents this
 * node to return undefined cache errors during replication when other nodes join this one.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Sanne Grinovero
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 * @author Fabio Massimo Ercoli
 *
 * @param <EK> the entity cache key type
 * @param <AK> the association cache key type
 * @param <ISK> the identity source cache key type
 */
public abstract class LocalCacheManager<EK, AK, ISK> {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private final EmbeddedCacheManager cacheManager;
	private final boolean isProvidedCacheManager;

	protected LocalCacheManager(EmbeddedCacheManager cacheManager) {
		this.cacheManager = cacheManager;
		this.isProvidedCacheManager = true;
	}

	protected LocalCacheManager(URL configUrl, JtaPlatform platform, Set<String> cacheNames, KeyProvider<EK, AK, ISK> keyProvider) {
		this.cacheManager = createCustomCacheManager( configUrl, platform, cacheNames, keyProvider );
		this.isProvidedCacheManager = false;
	}

	private static EmbeddedCacheManager createCustomCacheManager(URL configUrl, JtaPlatform platform, Set<String> cacheNames, KeyProvider<?, ?, ?> keyProvider) {
		Set<String> allCacheNames = new HashSet<>();//To include both the requires ones and the ones found in the configuration files
		allCacheNames.addAll( cacheNames );
		TransactionManagerLookupDelegator transactionManagerLookupDelegator = new TransactionManagerLookupDelegator( platform );
		try {
			InputStream configurationFile = configUrl.openStream();
			try {
				InfinispanConfigurationParser ispnConfiguration = new InfinispanConfigurationParser();
				ConfigurationBuilderHolder configurationBuilderHolder = ispnConfiguration.parseFile( configurationFile );
				EmbeddedCacheManager tmpCacheManager = new DefaultCacheManager( configurationBuilderHolder, false );

				// override global configuration from the config file to inject externalizers
				SerializationConfigurationBuilder serializationConfiguration = configurationBuilderHolder
					.getGlobalConfigurationBuilder()
					.serialization();

				ExternalizersIntegration.registerOgmExternalizers( serializationConfiguration );
				allCacheNames.addAll( tmpCacheManager.getCacheNames() );

				GlobalConfiguration globalConfiguration = serializationConfiguration.build();
				Configuration defaultCacheConfiguration = defaultCacheConfiguration( transactionManagerLookupDelegator, tmpCacheManager, globalConfiguration );

				EmbeddedCacheManager cacheManager = new DefaultCacheManager( globalConfiguration, defaultCacheConfiguration, false );

				// override the named cache configuration defined in the configuration file to
				// inject the platform TransactionManager
				for ( String cacheName : allCacheNames ) {
					if ( !isDefaultCacheName( globalConfiguration, cacheName ) ) {
						Configuration config = tmpCacheManager.getCacheConfiguration( cacheName );
						if ( config == null ) {
							config = defaultCacheConfiguration;
						}
						else {
							config = updateConfiguration( cacheName, transactionManagerLookupDelegator, config );
						}
						if ( config == null ) {
							throw LOG.missingCacheConfiguration( cacheName );
						}
						cacheManager.defineConfiguration( cacheName, config );
					}
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

	private static Configuration defaultCacheConfiguration(TransactionManagerLookupDelegator transactionManagerLookupDelegator, EmbeddedCacheManager tmpCacheManager, GlobalConfiguration globalConfiguration) {
		if ( tmpCacheManager.getDefaultCacheConfiguration() != null ) {
			String defaultCacheName = globalConfiguration.defaultCacheName().orElse( null );
			return updateConfiguration( defaultCacheName, transactionManagerLookupDelegator, tmpCacheManager.getDefaultCacheConfiguration() );
		}
		return null;
	}

	private static Configuration updateConfiguration(String cacheName, TransactionManagerLookupDelegator transactionManagerLookupDelegator, Configuration configuration) {
		Configuration newConfiguration = enableClusteringHashGroups( cacheName, configuration );
		newConfiguration = injectTransactionManager( transactionManagerLookupDelegator, newConfiguration );
		return newConfiguration;
	}

	private static boolean isDefaultCacheName(GlobalConfiguration globalConfiguration, String cacheName) {
		String defaultCacheName = globalConfiguration.defaultCacheName().orElse( null );
		return cacheName.equals( defaultCacheName );
	}

	/**
	 * Enable the clustering.hash.groups configuration if it's not already enabled.
	 * <p>
	 * Infinispan requires this option enabled because we are using fine grained maps.
	 * The function will log a warning if the property is disabled.
	 *
	 * @return the updated configuration
	 */
	private static Configuration enableClusteringHashGroups(String cacheName, Configuration configuration) {
		if ( configuration.clustering().hash().groups().enabled() ) {
			return configuration;
		}
		LOG.clusteringHashGroupsMustBeEnabled( cacheName );
		ConfigurationBuilder builder = new ConfigurationBuilder().read( configuration );
		builder.clustering().hash().groups().enabled();
		return builder.build();
	}

	/**
	 * Inject our TransactionManager lookup delegate for transactional caches ONLY!
	 * <p>
	 * injecting one in a non-transactional cache will have side-effects on other configuration settings.
	 *
	 * @return an updated version of the configuration
	 */
	private static Configuration injectTransactionManager(TransactionManagerLookupDelegator transactionManagerLookupDelegator, Configuration configuration) {
		if ( configuration.transaction().transactionMode() == TransactionMode.TRANSACTIONAL ) {
			ConfigurationBuilder builder = new ConfigurationBuilder().read( configuration );
			builder.transaction()
					.transactionManagerLookup( transactionManagerLookupDelegator );
			return builder.build();
		}
		return configuration;
	}

	private static HibernateException raiseConfigurationError(Exception e, String cfgName) {
		return new HibernateException(
				"Could not start Infinispan CacheManager using as configuration file: " + cfgName, e
		);
	}

	public EmbeddedCacheManager getCacheManager() {
		return cacheManager;
	}

	public void stop() {
		if ( !isProvidedCacheManager ) {
			cacheManager.stop();
		}
	}

	public abstract Cache<EK, Map<String, Object>> getEntityCache(EntityKeyMetadata keyMetadata);

	public abstract Cache<AK, Map<RowKey, Map<String, Object>>> getAssociationCache(AssociationKeyMetadata keyMetadata);

	public abstract Cache<ISK, Object> getIdSourceCache(IdSourceKeyMetadata keyMetadata);

	/**
	 * Groups the given entity types by the caches they are stored in.
	 *
	 * @param entityKeyMetadatas the meta-data of the entities
	 * @return the {@link Bucket}s containg the entities corressponding to the entity key meta-datas
	 */
	public abstract Set<Bucket<EK>> getWorkBucketsFor(EntityKeyMetadata... entityKeyMetadatas);

	/**
	 * Describe all the entity key meta-data that work on a given cache
	 */
	public static class Bucket<EK> {

		private final Cache<EK, Map<String, Object>> cache;
		private final EntityKeyMetadata[] entityKeyMetadatas;

		public Bucket(Cache<EK, Map<String,Object>> cache, List<EntityKeyMetadata> entityKeyMetadatas) {
			this.cache = cache;
			this.entityKeyMetadatas = entityKeyMetadatas.toArray( new EntityKeyMetadata[entityKeyMetadatas.size()] );
		}

		public Bucket(Cache<EK, Map<String,Object>> cache, EntityKeyMetadata... entityKeyMetadatas) {
			this.cache = cache;
			this.entityKeyMetadatas = entityKeyMetadatas;
		}

		public Cache<EK, Map<String, Object>> getCache() {
			return cache;
		}

		public EntityKeyMetadata[] getEntityKeyMetadata() {
			return entityKeyMetadatas;
		}
	}
}
