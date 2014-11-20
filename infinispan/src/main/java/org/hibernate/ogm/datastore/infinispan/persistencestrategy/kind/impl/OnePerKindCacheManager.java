/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.impl;

import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.infinispan.impl.CacheNames;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.LocalCacheManager;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * A {@link LocalCacheManager} which uses one cache for all entities, one cache for all associations and one cache for
 * all id sources.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public class OnePerKindCacheManager extends LocalCacheManager<EntityKey, AssociationKey, IdSourceKey> {

	private final Cache<EntityKey, Map<String, Object>> entityCache;
	private final Cache<AssociationKey, Map<RowKey, Map<String, Object>>> associationCache;
	private final Cache<IdSourceKey, Object> idSourceCache;

	public OnePerKindCacheManager(EmbeddedCacheManager cacheManager) {
		super( cacheManager );

		entityCache = getCacheManager().getCache( CacheNames.ENTITY_CACHE );
		associationCache = getCacheManager().getCache( CacheNames.ASSOCIATION_CACHE );
		idSourceCache = getCacheManager().getCache( CacheNames.IDENTIFIER_CACHE );
	}

	public OnePerKindCacheManager(URL configUrl, JtaPlatform platform, Set<EntityKeyMetadata> entityTypes, OnePerKindCacheAndKeyProvider keyProvider) {
		super( configUrl, platform, entityTypes, keyProvider );

		entityCache = getCacheManager().getCache( CacheNames.ENTITY_CACHE );
		associationCache = getCacheManager().getCache( CacheNames.ASSOCIATION_CACHE );
		idSourceCache = getCacheManager().getCache( CacheNames.IDENTIFIER_CACHE );
	}

	@Override
	public Cache<EntityKey, Map<String, Object>> getEntityCache(EntityKeyMetadata keyMetadata) {
		return entityCache;
	}

	@Override
	public Cache<AssociationKey, Map<RowKey, Map<String, Object>>> getAssociationCache(AssociationKeyMetadata keyMetadata) {
		return associationCache;
	}

	@Override
	public Cache<IdSourceKey, Object> getIdSourceCache(IdSourceKeyMetadata keyMetadata) {
		return idSourceCache;
	}

	@Override
	public Set<Bucket> getWorkBucketsFor(EntityKeyMetadata... entityKeyMetadatas) {
		Set<Bucket> result = new HashSet<Bucket>();
		Bucket bucket = new Bucket( entityCache, entityKeyMetadatas );
		result.add( bucket );
		return result;
	}
}
