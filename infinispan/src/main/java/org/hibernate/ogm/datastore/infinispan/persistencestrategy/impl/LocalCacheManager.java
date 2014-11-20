/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.infinispan.impl.CacheAndKeyProvider;
import org.hibernate.ogm.datastore.infinispan.impl.CacheNames;
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
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Provides access to the ISPN caches used for storing entities, associations and id sources. The number of caches used
 * depends on the specific implementation of this contract.
 * <p>
 * Implementations need to make sure all needed caches are started before state transfer happens. This prevents this
 * node to return undefined cache errors during replication when other nodes join this one.
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

	protected LocalCacheManager(URL configUrl, JtaPlatform platform, Set<EntityKeyMetadata> entityTypes, CacheAndKeyProvider<EK, AK, ISK> keyProvider) {
		this.cacheManager = createCustomCacheManager( configUrl, platform, entityTypes, keyProvider );
		this.isProvidedCacheManager = false;
	}

	private static EmbeddedCacheManager createCustomCacheManager(URL configUrl, JtaPlatform platform, Set<EntityKeyMetadata> entityTypes, CacheAndKeyProvider<?, ?, ?> keyProvider) {
		TransactionManagerLookupDelegator transactionManagerLookupDelegator = new TransactionManagerLookupDelegator( platform );
		try {
			InputStream configurationFile = configUrl.openStream();
			try {
				EmbeddedCacheManager tmpCacheManager = new DefaultCacheManager( configurationFile, false );

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
				for (EntityKeyMetadata entityType : entityTypes ) {
					Configuration originalCfg = tmpCacheManager.getCacheConfiguration( entityType.getTable() );
					if ( originalCfg == null ) {
						originalCfg = tmpCacheManager.getDefaultCacheConfiguration();
					}
					Configuration newCfg = new ConfigurationBuilder()
						.read( originalCfg )
							.transaction()
								.transactionManagerLookup( transactionManagerLookupDelegator )
						.build();
					cacheManager.defineConfiguration( entityType.getTable(), newCfg );
				}

				Configuration originalCfg2 = tmpCacheManager.getCacheConfiguration( CacheNames.ENTITY_CACHE );
				Configuration newCfg2 = new ConfigurationBuilder()
					.read( originalCfg2 )
						.transaction()
							.transactionManagerLookup( transactionManagerLookupDelegator )
					.build();
				cacheManager.defineConfiguration( CacheNames.ENTITY_CACHE, newCfg2 );

				Configuration originalCfg = tmpCacheManager.getCacheConfiguration( CacheNames.ASSOCIATION_CACHE );
				Configuration newCfg = new ConfigurationBuilder()
					.read( originalCfg )
						.transaction()
							.transactionManagerLookup( transactionManagerLookupDelegator )
					.build();
				cacheManager.defineConfiguration( CacheNames.ASSOCIATION_CACHE, newCfg );

				Configuration originalCfg1 = tmpCacheManager.getCacheConfiguration( CacheNames.IDENTIFIER_CACHE );
				Configuration newCfg1 = new ConfigurationBuilder()
					.read( originalCfg1 )
						.transaction()
							.transactionManagerLookup( transactionManagerLookupDelegator )
					.build();
				cacheManager.defineConfiguration( CacheNames.IDENTIFIER_CACHE, newCfg1 );


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

	public abstract Set<Bucket> getWorkBucketsFor(EntityKeyMetadata... entityKeyMetadatas);

	/**
	 * Describe all the entity key metadata that work on a given cache
	 */
	public static class Bucket<EK> {
		private final Cache<EK, Map<String,Object>> cache;
		private final List<EntityKeyMetadata> entityKeyMetadatas;

		public Bucket(Cache<EK, Map<String,Object>> cache) {
			this.cache = cache;
			this.entityKeyMetadatas = new ArrayList<EntityKeyMetadata>();
		}

		public Bucket(Cache<EK, Map<String,Object>> cache, EntityKeyMetadata... entityKeyMetadatas) {
			this.cache = cache;
			this.entityKeyMetadatas = Arrays.asList( entityKeyMetadatas );
		}

		public Cache getCache() {
			return cache;
		}

		public EntityKeyMetadata[] getEntityKeyMetadata() {
			return entityKeyMetadatas.toArray( new EntityKeyMetadata[ entityKeyMetadatas.size() ] );
		}

		public void addEntityKeyMetadata(EntityKeyMetadata entityKeyMetadata) {
			this.entityKeyMetadatas.add( entityKeyMetadata );
		}
	}
}
