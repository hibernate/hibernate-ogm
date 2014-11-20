/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.impl;

import static org.hibernate.ogm.util.impl.CollectionHelper.newConcurrentHashMap;

import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.LocalCacheManager;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.impl.CacheNames;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentEntityKey;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * A {@link LocalCacheManager} which uses a dedicated {@link Cache} per entity/association/id source table.
 *
 * @author Gunnar Morling
 */
public class PerTableCacheManager extends LocalCacheManager<PersistentEntityKey, AssociationKey, IdSourceKey> {

	private final ConcurrentMap<String, Cache<PersistentEntityKey, Map<String, Object>>> entityCaches;

	public PerTableCacheManager(EmbeddedCacheManager cacheManager, Set<EntityKeyMetadata> entityTypes) {
		super( cacheManager );

		entityCaches = initializeEntityCaches( getCacheManager(), entityTypes );
	}

	public PerTableCacheManager(URL configUrl, JtaPlatform platform, Set<EntityKeyMetadata> entityTypes) {
		super( configUrl, platform, getCacheNames ( entityTypes ), new PerTableKeyProvider() );

		entityCaches = initializeEntityCaches( getCacheManager(), entityTypes );
	}

	private static Set<String> getCacheNames(Set<EntityKeyMetadata> entityTypes) {
		Set<String> cacheNames = new HashSet<String>();

		for ( EntityKeyMetadata entityKeyMetadata : entityTypes ) {
			cacheNames.add( entityKeyMetadata.getTable() );
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

	@Override
	public Cache<PersistentEntityKey, Map<String, Object>> getEntityCache(EntityKeyMetadata keyMetadata) {
		return entityCaches.get( keyMetadata.getTable() );
	}

	@Override
	public Cache<AssociationKey, Map<RowKey, Map<String, Object>>> getAssociationCache(AssociationKeyMetadata keyMetadata) {
		// TODO Use cache per table
		return getCacheManager().getCache( CacheNames.ASSOCIATION_CACHE );
	}

	@Override
	public Cache<IdSourceKey, Object> getIdSourceCache(IdSourceKeyMetadata keyMetadata) {
		// TODO Use cache per table
		return getCacheManager().getCache( CacheNames.IDENTIFIER_CACHE );
	}

	@Override
	public Set<LocalCacheManager.Bucket> getWorkBucketsFor(EntityKeyMetadata... entityKeyMetadatas) {
		Set<Bucket> result = new HashSet<Bucket>();

		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
			Bucket bucket = new Bucket( getEntityCache( entityKeyMetadata ), entityKeyMetadatas );
			result.add( bucket );
		}

		return result;
	}
}
