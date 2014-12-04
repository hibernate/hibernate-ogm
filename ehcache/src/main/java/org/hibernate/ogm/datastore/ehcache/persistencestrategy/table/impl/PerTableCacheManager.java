/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.persistencestrategy.table.impl;

import static org.hibernate.ogm.util.impl.CollectionHelper.newConcurrentHashMap;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;

import org.hibernate.ogm.datastore.ehcache.impl.Cache;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.common.impl.CacheNames;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl.LocalCacheManager;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;

/**
 * A {@link LocalCacheManager} which uses a dedicated {@link Cache} per entity/association/id source table.
 *
 * @author Gunnar Morling
 */
public class PerTableCacheManager extends LocalCacheManager<PerTableSerializableEntityKey, PerTableSerializableAssociationKey, PerTableSerializableIdSourceKey> {

	private static final String ASSOCIATIONS_CACHE_PREFIX = "associations_";

	private final ConcurrentMap<String, Cache<PerTableSerializableEntityKey>> entityCaches;
	private final ConcurrentMap<String, Cache<PerTableSerializableAssociationKey>> associationCaches;
	private final ConcurrentMap<String, Cache<PerTableSerializableIdSourceKey>> idSourceCaches;

	public PerTableCacheManager(CacheManager cacheManager, Set<EntityKeyMetadata> entityTypes, Set<AssociationKeyMetadata> associationTypes, Set<IdSourceKeyMetadata> idSourceTypes) {
		super( cacheManager );

		entityCaches = initializeEntityCaches( cacheManager, entityTypes );
		associationCaches = initializeAssociationCaches( cacheManager, associationTypes );
		idSourceCaches = initializeIdSourceCaches( cacheManager, idSourceTypes );
	}

	private static ConcurrentMap<String, Cache<PerTableSerializableEntityKey>> initializeEntityCaches(CacheManager embeddedCacheManager, Set<EntityKeyMetadata> entityTypes) {
		ConcurrentHashMap<String, Cache<PerTableSerializableEntityKey>> entityCaches = newConcurrentHashMap( entityTypes.size() );
		for ( EntityKeyMetadata entityKeyMetadata : entityTypes ) {
			String cacheName = entityKeyMetadata.getTable();
			entityCaches.put(
					cacheName,
					new Cache<PerTableSerializableEntityKey>( getCache( embeddedCacheManager, cacheName, CacheNames.ENTITY_CACHE ) )
			);
		}

		return entityCaches;
	}

	private static ConcurrentMap<String, Cache<PerTableSerializableAssociationKey>> initializeAssociationCaches(CacheManager embeddedCacheManager, Set<AssociationKeyMetadata> associationTypes) {
		ConcurrentHashMap<String, Cache<PerTableSerializableAssociationKey>> associationCaches = newConcurrentHashMap( associationTypes.size() );
		for ( AssociationKeyMetadata associationKeyMetadata : associationTypes ) {
			String cacheName = getCacheName( associationKeyMetadata );

			associationCaches.put(
					cacheName,
					new Cache<PerTableSerializableAssociationKey>( getCache( embeddedCacheManager, cacheName, CacheNames.ASSOCIATION_CACHE ) )
			);
		}

		return associationCaches;
	}

	private static ConcurrentMap<String, Cache<PerTableSerializableIdSourceKey>> initializeIdSourceCaches(CacheManager embeddedCacheManager, Set<IdSourceKeyMetadata> idSourceTypes) {
		ConcurrentMap<String, Cache<PerTableSerializableIdSourceKey>> idSourceCaches = newConcurrentHashMap( idSourceTypes.size() );
		for ( IdSourceKeyMetadata idSourceKeyMetadata : idSourceTypes ) {
			String cacheName = idSourceKeyMetadata.getName();
			if ( !idSourceCaches.containsKey( cacheName ) ) {
				idSourceCaches.put(
						cacheName,
						new Cache<PerTableSerializableIdSourceKey>( getCache( embeddedCacheManager, cacheName, CacheNames.IDENTIFIER_CACHE ) )
				);
			}
		}

		return idSourceCaches;
	}

	/**
	 * Gets the cache with the given name from Ehache.
	 * <p>
	 * If no cache with that name exists, one will be created and registered, using the configuration from the cache
	 * with the given template name.
	 */
	private static net.sf.ehcache.Cache getCache(CacheManager embeddedCacheManager, String cacheName, String templateName) {
		net.sf.ehcache.Cache cache = embeddedCacheManager.getCache( cacheName );

		if ( cache == null ) {
			CacheConfiguration configuration = embeddedCacheManager.getConfiguration().getCacheConfigurations().get( templateName ).clone();
			configuration.setName( cacheName );
			cache = new net.sf.ehcache.Cache( configuration );
			embeddedCacheManager.addCache( cache );
		}

		return cache;
	}

	@Override
	public Cache<PerTableSerializableEntityKey> getEntityCache(EntityKeyMetadata keyMetadata) {
		return entityCaches.get( keyMetadata.getTable() );
	}

	@Override
	public Cache<PerTableSerializableAssociationKey> getAssociationCache(AssociationKeyMetadata keyMetadata) {
		return associationCaches.get( getCacheName( keyMetadata ) );
	}

	@Override
	public Cache<PerTableSerializableIdSourceKey> getIdSourceCache(IdSourceKeyMetadata keyMetadata) {
		return idSourceCaches.get( keyMetadata.getName() );
	}

	private static String getCacheName(AssociationKeyMetadata keyMetadata) {
		return ASSOCIATIONS_CACHE_PREFIX + keyMetadata.getTable();
	}

	@Override
	public void forEachTuple(KeyProcessor<PerTableSerializableEntityKey> processor, EntityKeyMetadata... entityKeyMetadatas) {
		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
			Cache<PerTableSerializableEntityKey> entityCache = getEntityCache( entityKeyMetadata );

			for ( PerTableSerializableEntityKey key : entityCache.getKeys() ) {
				processor.processKey( key, entityCache );
			}
		}
	}
}
