/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl;

import java.util.Set;

import net.sf.ehcache.CacheManager;

import org.hibernate.ogm.datastore.ehcache.persistencestrategy.kind.impl.OnePerKindCacheManager;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.kind.impl.OnePerKindKeyProvider;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.kind.impl.SerializableAssociationKey;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.kind.impl.SerializableEntityKey;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.kind.impl.SerializableIdSourceKey;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.table.impl.PerTableCacheManager;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.table.impl.PerTableKeyProvider;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.table.impl.PerTableSerializableAssociationKey;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.table.impl.PerTableSerializableEntityKey;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.table.impl.PerTableSerializableIdSourceKey;
import org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;

/**
 * A strategy for persisting entities, associations and id sources in EhCache. Depending on the specific strategy,
 * shared or specific caches will be used, and different key types will be used to represent key objects.
 *
 * @author Gunnar Morling
 *
 * @param <EK> the entity cache key type
 * @param <AK> the association cache key type
 * @param <ISK> the identity source cache key type
 */
public class PersistenceStrategy<EK, AK, ISK> {

	private final LocalCacheManager<EK, AK, ISK> cacheManager;
	private final KeyProvider<EK, AK, ISK> keyProvider;

	private PersistenceStrategy(LocalCacheManager<EK, AK, ISK> cacheManager, KeyProvider<EK, AK, ISK> keyProvider) {
		this.cacheManager = cacheManager;
		this.keyProvider = keyProvider;
	}

	/**
	 * Returns a persistence strategy based on the passed configuration.
	 *
	 * @param cacheMapping the selected {@link org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType}
	 * @param externalCacheManager the Ehcache cache manager
	 * @param entityTypes the meta-data of the entities
	 * @param associationTypes the meta-data of the associations
	 * @param idSourceTypes the meta-data of the id generators
	 * @return the persistence strategy
	 */
	public static PersistenceStrategy<?, ?, ?> getInstance(
			CacheMappingType cacheMapping,
			CacheManager externalCacheManager,
			Set<EntityKeyMetadata> entityTypes,
			Set<AssociationKeyMetadata> associationTypes,
			Set<IdSourceKeyMetadata> idSourceTypes ) {

		if ( cacheMapping == CacheMappingType.CACHE_PER_KIND ) {
			return getPerKindStrategy( externalCacheManager );
		}
		else {
			return getPerTableStrategy( externalCacheManager, entityTypes, associationTypes, idSourceTypes );
		}
	}

	/**
	 * Returns the "per-kind" persistence strategy. Three caches will be used: one for entities, one for associations
	 * and one for id sources.
	 */
	private static PersistenceStrategy<?, ?, ?> getPerKindStrategy(CacheManager externalCacheManager) {
		OnePerKindKeyProvider keyProvider = new OnePerKindKeyProvider();
		OnePerKindCacheManager cacheManager = new OnePerKindCacheManager( externalCacheManager );

		return new PersistenceStrategy<SerializableEntityKey, SerializableAssociationKey, SerializableIdSourceKey>( cacheManager, keyProvider );
	}

	/**
	 * Returns the "per-table" persistence strategy, i.e. one dedicated cache will be used for each
	 * entity/association/id source table.
	 */
	private static PersistenceStrategy<?, ?, ?> getPerTableStrategy(
			CacheManager externalCacheManager,
			Set<EntityKeyMetadata> entityTypes,
			Set<AssociationKeyMetadata> associationTypes,
			Set<IdSourceKeyMetadata> idSourceTypes) {

		PerTableKeyProvider keyProvider = new PerTableKeyProvider();
		PerTableCacheManager cacheManager = new PerTableCacheManager( externalCacheManager, entityTypes, associationTypes, idSourceTypes );

		return new PersistenceStrategy<PerTableSerializableEntityKey, PerTableSerializableAssociationKey, PerTableSerializableIdSourceKey>( cacheManager, keyProvider );
	}

	/**
	 * Returns the {@link LocalCacheManager} of this strategy, providing access to the actual ISPN caches.
	 *
	 * @return the cache manager
	 */
	public LocalCacheManager<EK, AK, ISK> getCacheManager() {
		return cacheManager;
	}

	/**
	 * Returns the {@link KeyProvider} of this strategy, converting OGM core's key objects into the keys persisted in
	 * the datastore.
	 *
	 * @return the key provider
	 */
	public KeyProvider<EK, AK, ISK> getKeyProvider() {
		return keyProvider;
	}
}
