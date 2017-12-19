/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.impl;

import static org.hibernate.ogm.util.impl.CollectionHelper.newConcurrentHashMap;
import static org.hibernate.ogm.util.impl.CollectionHelper.newHashMap;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.LocalCacheManager;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentAssociationKey;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentEntityKey;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentIdSourceKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * A {@link LocalCacheManager} which uses a dedicated {@link Cache} per entity/association/id source table.
 *
 * @author Gunnar Morling
 */
public class PerTableCacheManager extends LocalCacheManager<PersistentEntityKey, PersistentAssociationKey, PersistentIdSourceKey> {

	private static final String ASSOCIATIONS_CACHE_PREFIX = "associations_";

	private final ConcurrentMap<String, Cache<PersistentEntityKey, Map<String, Object>>> entityCaches;
	private final ConcurrentMap<String, Cache<PersistentAssociationKey, Map<RowKey, Map<String, Object>>>> associationCaches;
	private final ConcurrentMap<String, Cache<PersistentIdSourceKey, Object>> idSourceCaches;

	public PerTableCacheManager(EmbeddedCacheManager cacheManager, Set<EntityKeyMetadata> entityTypes, Set<AssociationKeyMetadata> associationTypes, Set<IdSourceKeyMetadata> idSourceTypes) {
		super( cacheManager );

		entityCaches = initializeEntityCaches( getCacheManager(), entityTypes );
		associationCaches = initializeAssociationCaches( getCacheManager(), associationTypes );
		idSourceCaches = initializeIdSourceCaches( getCacheManager(), idSourceTypes );
	}

	public PerTableCacheManager(URL configUrl, JtaPlatform platform, Set<EntityKeyMetadata> entityTypes, Set<AssociationKeyMetadata> associationTypes, Set<IdSourceKeyMetadata> idSourceTypes) {
		super( configUrl, platform, getCacheNames( entityTypes, associationTypes, idSourceTypes ), new PerTableKeyProvider() );

		entityCaches = initializeEntityCaches( getCacheManager(), entityTypes );
		associationCaches = initializeAssociationCaches( getCacheManager(), associationTypes );
		idSourceCaches = initializeIdSourceCaches( getCacheManager(), idSourceTypes );
	}

	private static Set<String> getCacheNames(Set<EntityKeyMetadata> entityTypes, Set<AssociationKeyMetadata> associationTypes, Set<IdSourceKeyMetadata> idSourceTypes) {
		Set<String> cacheNames = new HashSet<String>();

		for ( EntityKeyMetadata entityKeyMetadata : entityTypes ) {
			cacheNames.add( entityKeyMetadata.getTable() );
		}

		for ( AssociationKeyMetadata associationKeyMetadata : associationTypes ) {
			cacheNames.add( getCacheName( associationKeyMetadata ) );
		}

		for ( IdSourceKeyMetadata idSourceKeyMetadata : idSourceTypes ) {
			cacheNames.add( idSourceKeyMetadata.getName() );
		}

		return cacheNames;
	}

	private static ConcurrentMap<String, Cache<PersistentEntityKey, Map<String,Object>>> initializeEntityCaches(EmbeddedCacheManager embeddedCacheManager, Set<EntityKeyMetadata> entityTypes) {
		ConcurrentHashMap<String, Cache<PersistentEntityKey, Map<String, Object>>> entityCaches = newConcurrentHashMap( entityTypes.size() );
		for ( EntityKeyMetadata entityKeyMetadata : entityTypes ) {
			Cache<PersistentEntityKey, Map<String, Object>> entityCache = embeddedCacheManager.getCache( entityKeyMetadata.getTable() );
			entityCaches.put( entityKeyMetadata.getTable(), entityCache );
		}

		return entityCaches;
	}

	private static ConcurrentMap<String, Cache<PersistentAssociationKey, Map<RowKey, Map<String, Object>>>> initializeAssociationCaches(EmbeddedCacheManager embeddedCacheManager, Set<AssociationKeyMetadata> associationTypes) {
		ConcurrentHashMap<String, Cache<PersistentAssociationKey, Map<RowKey, Map<String, Object>>>> associationCaches = newConcurrentHashMap( associationTypes.size() );
		for ( AssociationKeyMetadata associationKeyMetadata : associationTypes ) {
			String cacheName = getCacheName( associationKeyMetadata );

			associationCaches.put(
					cacheName,
					embeddedCacheManager.<PersistentAssociationKey, Map<RowKey, Map<String, Object>>>getCache( cacheName )
			);
		}

		return associationCaches;
	}

	private static ConcurrentMap<String, Cache<PersistentIdSourceKey, Object>> initializeIdSourceCaches(EmbeddedCacheManager embeddedCacheManager, Set<IdSourceKeyMetadata> idSourceTypes) {
		ConcurrentMap<String, Cache<PersistentIdSourceKey, Object>> idSourceCaches = newConcurrentHashMap( idSourceTypes.size() );
		for ( IdSourceKeyMetadata idSourceKeyMetadata : idSourceTypes ) {
			if ( !idSourceCaches.containsKey( idSourceKeyMetadata.getName() ) ) {
				Cache<PersistentIdSourceKey, Object> idSourceCache = embeddedCacheManager.getCache( idSourceKeyMetadata.getName() );
				idSourceCaches.put( idSourceKeyMetadata.getName(), idSourceCache );
			}
		}

		return idSourceCaches;
	}

	@Override
	public Cache<PersistentEntityKey, Map<String, Object>> getEntityCache(EntityKeyMetadata keyMetadata) {
		return entityCaches.get( keyMetadata.getTable() );
	}

	@Override
	public Cache<PersistentAssociationKey, Map<RowKey, Map<String, Object>>> getAssociationCache(AssociationKeyMetadata keyMetadata) {
		return associationCaches.get( getCacheName( keyMetadata ) );
	}

	@Override
	public Cache<PersistentIdSourceKey, Object> getIdSourceCache(IdSourceKeyMetadata keyMetadata) {
		return idSourceCaches.get( keyMetadata.getName() );
	}

	@Override
	public Set<Bucket<PersistentEntityKey>> getWorkBucketsFor(EntityKeyMetadata... entityKeyMetadatas) {
		Map<String, List<EntityKeyMetadata>> metadataByTable = groupByTable( entityKeyMetadatas );
		Set<Bucket<PersistentEntityKey>> result = new HashSet<Bucket<PersistentEntityKey>>();

		for ( Entry<String, List<EntityKeyMetadata>> entry : metadataByTable.entrySet() ) {
			result.add( new Bucket<PersistentEntityKey>( entityCaches.get( entry.getKey() ), entry.getValue() ) );
		}

		return result;
	}

	private Map<String, List<EntityKeyMetadata>> groupByTable(EntityKeyMetadata... entityKeyMetadatas) {
		Map<String, List<EntityKeyMetadata>> metadataByTable = newHashMap();

		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
			List<EntityKeyMetadata> metadataOfTable = metadataByTable.get( entityKeyMetadata.getTable() );

			if ( metadataOfTable == null ) {
				metadataOfTable = new ArrayList<EntityKeyMetadata>();
				metadataByTable.put( entityKeyMetadata.getTable(), metadataOfTable );
			}

			metadataOfTable.add( entityKeyMetadata );
		}

		return metadataByTable;
	}

	private static String getCacheName(AssociationKeyMetadata keyMetadata) {
		return ASSOCIATIONS_CACHE_PREFIX + keyMetadata.getTable();
	}
}
