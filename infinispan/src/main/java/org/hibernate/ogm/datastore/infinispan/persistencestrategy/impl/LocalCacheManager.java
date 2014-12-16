/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.infinispan.impl.TransactionManagerLookupDelegator;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.infinispan.Cache;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.SerializationConfigurationBuilder;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

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
 *
 * @param <EK> the entity cache key type
 * @param <AK> the association cache key type
 * @param <ISK> the identity source cache key type
 */
public abstract class LocalCacheManager<EK, AK, ISK> {

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
		TransactionManagerLookupDelegator transactionManagerLookupDelegator = new TransactionManagerLookupDelegator( platform );
		try {
			InputStream configurationFile = configUrl.openStream();
			try {
				InfinispanConfigurationParser ispnConfiguration = new InfinispanConfigurationParser();
				ConfigurationBuilderHolder configurationBuilderHolder = ispnConfiguration.parseFile( configurationFile );
				EmbeddedCacheManager tmpCacheManager = new DefaultCacheManager( configurationBuilderHolder, false );

				// override global configuration from the config file to inject externalizers
				SerializationConfigurationBuilder serializationConfiguration = new GlobalConfigurationBuilder()
					.read( tmpCacheManager.getCacheManagerConfiguration() )
					.serialization();

				for ( AdvancedExternalizer<?> externalizer : keyProvider.getExternalizers() ) {
					serializationConfiguration.addAdvancedExternalizer( externalizer.getId(), externalizer );
				}

				GlobalConfiguration globalConfiguration = serializationConfiguration.build();

				EmbeddedCacheManager cacheManager = new DefaultCacheManager( globalConfiguration, false );

				// override the named cache configuration defined in the configuration file to
				// inject the platform TransactionManager
				for ( String cacheName : cacheNames ) {
					Configuration originalCfg = tmpCacheManager.getCacheConfiguration( cacheName );
					if ( originalCfg == null ) {
						originalCfg = tmpCacheManager.getDefaultCacheConfiguration();
					}
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

	private static HibernateException raiseConfigurationError(Exception e, String cfgName) {
		return new HibernateException(
				"Could not start Infinispan CacheManager using as configuration file: " + cfgName, e
		);
	}

	protected EmbeddedCacheManager getCacheManager() {
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
