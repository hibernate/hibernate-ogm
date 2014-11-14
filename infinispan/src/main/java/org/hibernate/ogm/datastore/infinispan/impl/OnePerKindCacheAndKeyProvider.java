/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispan.impl;

import java.util.HashSet;
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
 * Initial strategy that uses three caches. One for entities, one for associations and one for the identity sources.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
//TODO Consider the move the InfinispanDatastoreProvider#getCache from the provider
public class OnePerKindCacheAndKeyProvider implements CacheAndKeyProvider {

	private InfinispanDatastoreProvider provider;

	@Override
	public void configure(InfinispanDatastoreProvider provider) {
		this.provider = provider;
	}

	@Override
	public Cache<EntityKey, java.util.Map<String, Object>> getCacheForEntity(EntityKeyMetadata keyMetadata) {
		return provider.getCache( CacheNames.ENTITY_CACHE );
	}

	@Override
	public EntityKey getEntityCacheKey(EntityKey key) {
		return key;
	}

	@Override
	public Cache<AssociationKey, Map<RowKey, Map<String, Object>>> getCacheForAssociation(AssociationKeyMetadata keyMetadata) {
		return provider.getCache( CacheNames.ASSOCIATION_CACHE );
	}

	@Override
	public AssociationKey getAssociationCacheKey(AssociationKey key) {
		return key;
	}

	@Override
	public Cache<IdSourceKey, Object> getCacheForIdSource(IdSourceKeyMetadata keyMetadata) {
		return provider.getCache( CacheNames.IDENTIFIER_CACHE );
	}

	@Override
	public IdSourceKey getIdSourceCacheKey(IdSourceKey key) {
		return key;
	}

	@Override
	public Set<Bucket> getWorkBucketsFor(EntityKeyMetadata... entityKeyMetadatas) {
		Set<Bucket> result = new HashSet<Bucket>();
		Bucket bucket = new Bucket( provider.getCache( CacheNames.ENTITY_CACHE ), entityKeyMetadatas );
		result.add( bucket );
		return result;
	}
}
