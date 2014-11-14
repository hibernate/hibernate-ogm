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
import org.infinispan.distexec.mapreduce.Mapper;

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
 * EK is the entity cache key type
 * AK is the association cache key type
 * ISK is the identity source cache key type
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface CacheAndKeyProvider<EK,AK,ISK> {

	void configure(InfinispanDatastoreProvider provider);

	Cache<EK, Map<String, Object>> getCacheForEntity(EntityKeyMetadata keyMetadata);

	EK getEntityCacheKey(EntityKey key);

	Cache<AK,Map<RowKey,Map<String,Object>>> getCacheForAssociation(AssociationKeyMetadata keyMetadata);

	AK getAssociationCacheKey(AssociationKey key);

	Cache<ISK, Object> getCacheForIdSource(IdSourceKeyMetadata keyMetadata);

	ISK getIdSourceCacheKey(IdSourceKey key);

	Set<Bucket> getWorkBucketsFor(EntityKeyMetadata... entityKeyMetadatas);

	Mapper<EK, Map<String, Object>, EK, Map<String, Object>> getMapper(EntityKeyMetadata... entityKeyMetadatas);

	/**
	 * Describe all the entity key metadata that work on a given cache
	 */
	public static class Bucket<EK> {
		private Cache<EK, Map<String,Object>> cache;
		private List<EntityKeyMetadata> entityKeyMetadatas;

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
