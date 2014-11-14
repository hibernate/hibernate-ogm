/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispan.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.infinispan.Cache;

import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;

/**
 * Abstracts the selection of caches for a given entity key, association key and identity source key.
 * Also offers conversion from the OGM keys to the cache keys.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface CacheAndKeyProvider {

	void configure(InfinispanDatastoreProvider provider);

	Cache<EntityKey, Map<String, Object>> getCacheForEntity(EntityKeyMetadata keyMetadata);

	EntityKey getEntityCacheKey(EntityKey key);

	Cache<AssociationKey,Map<RowKey,Map<String,Object>>> getCacheForAssociation(AssociationKeyMetadata keyMetadata);

	AssociationKey getAssociationCacheKey(AssociationKey key);

	Cache<IdSourceKey, Object> getCacheForIdSource(IdSourceKeyMetadata keyMetadata);

	IdSourceKey getIdSourceCacheKey(IdSourceKey key);

	Set<Bucket> getWorkBucketsFor(EntityKeyMetadata... entityKeyMetadatas);

	/**
	 * Describe all the entity key metadata that work on a given cache
	 */
	public static class Bucket {
		//TODO make it generic?
		private Cache cache;
		private List<EntityKeyMetadata> entityKeyMetadatas;

		public Bucket(Cache cache) {
			this.cache = cache;
			this.entityKeyMetadatas = new ArrayList<EntityKeyMetadata>();
		}

		public Bucket(Cache cache, EntityKeyMetadata... entityKeyMetadatas) {
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
