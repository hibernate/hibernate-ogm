/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.persistencestrategy.kind.impl;

import net.sf.ehcache.CacheManager;

import org.hibernate.ogm.datastore.ehcache.impl.Cache;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.common.impl.CacheNames;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl.LocalCacheManager;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;

/**
 * A {@link LocalCacheManager} which uses one cache for all entities, one cache for all associations and one cache for
 * all id sources.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public class OnePerKindCacheManager extends LocalCacheManager<SerializableEntityKey, SerializableAssociationKey, SerializableIdSourceKey> {

	private final Cache<SerializableEntityKey> entityCache;
	private final Cache<SerializableAssociationKey> associationCache;
	private final Cache<SerializableIdSourceKey> idSourceCache;

	public OnePerKindCacheManager(CacheManager cacheManager) {
		super( cacheManager );

		entityCache = new Cache<SerializableEntityKey>( cacheManager.getCache( CacheNames.ENTITY_CACHE ) );
		associationCache = new Cache<SerializableAssociationKey>( cacheManager.getCache( CacheNames.ASSOCIATION_CACHE ) );
		idSourceCache = new Cache<SerializableIdSourceKey>( cacheManager.getCache( CacheNames.IDENTIFIER_CACHE ) );
	}

	@Override
	public Cache<SerializableEntityKey> getEntityCache(EntityKeyMetadata keyMetadata) {
		return entityCache;
	}

	@Override
	public Cache<SerializableAssociationKey> getAssociationCache(AssociationKeyMetadata keyMetadata) {
		return associationCache;
	}

	@Override
	public Cache<SerializableIdSourceKey> getIdSourceCache(IdSourceKeyMetadata keyMetadata) {
		return idSourceCache;
	}

	@Override
	public void forEachTuple(KeyProcessor<SerializableEntityKey> processor, EntityKeyMetadata... entityKeyMetadatas) {
		for ( SerializableEntityKey key : entityCache.getKeys() ) {
			for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
				// Check if there is a way to load keys applying a filter
				if ( key.getTable().equals( entityKeyMetadata.getTable() ) ) {
					processor.processKey( key, entityCache );
				}
			}
		}
	}
}
