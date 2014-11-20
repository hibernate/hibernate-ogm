/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl;

import java.net.URL;
import java.util.Set;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.impl.OnePerKindCacheManager;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.impl.OnePerKindKeyProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentAssociationKey;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentEntityKey;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.impl.PerTableCacheManager;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.impl.PerTableKeyProvider;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * A strategy for persisting entities, associations and id sources in Infinispan. Depending on the specific strategy,
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
	 * Returns the "per-kind" persistence strategy. Three caches will be used: one for entities, one for associations
	 * and one for id sources.
	 */
	public static PersistenceStrategy<?, ?, ?> getPerKindStrategy(EmbeddedCacheManager externalCacheManager, URL configUrl, JtaPlatform platform, Set<EntityKeyMetadata> entityTypes) {
		OnePerKindKeyProvider keyProvider = new OnePerKindKeyProvider();

		OnePerKindCacheManager cacheManager = externalCacheManager != null ?
				new OnePerKindCacheManager( externalCacheManager ) :
				new OnePerKindCacheManager( configUrl, platform, keyProvider );

		return new PersistenceStrategy<EntityKey, AssociationKey, IdSourceKey>( cacheManager, keyProvider );
	}

	/**
	 * Returns the "per-table" persistence strategy, i.e. one dedicated cache will be used for each
	 * entity/association/id source table.
	 */
	public static PersistenceStrategy<?, ?, ?> getPerTableStrategy(EmbeddedCacheManager externalCacheManager, URL configUrl, JtaPlatform platform, Set<EntityKeyMetadata> entityTypes, Set<AssociationKeyMetadata> associationTypes) {
		PerTableKeyProvider keyProvider = new PerTableKeyProvider();

		PerTableCacheManager cacheManager = externalCacheManager != null ?
				new PerTableCacheManager( externalCacheManager, entityTypes, associationTypes ) :
				new PerTableCacheManager( configUrl, platform, entityTypes, associationTypes );

		return new PersistenceStrategy<PersistentEntityKey, PersistentAssociationKey, IdSourceKey>( cacheManager, keyProvider );
	}

	/**
	 * Returns the {@link LocalCacheManager} of this strategy, providing access to the actual ISPN caches.
	 */
	public LocalCacheManager<EK, AK, ISK> getCacheManager() {
		return cacheManager;
	}

	/**
	 * Returns the {@link KeyProvider} of this strategy, converting OGM core's key objects into the keys persisted in
	 * thed datastore.
	 */
	public KeyProvider<EK, AK, ISK> getKeyProvider() {
		return keyProvider;
	}
}
