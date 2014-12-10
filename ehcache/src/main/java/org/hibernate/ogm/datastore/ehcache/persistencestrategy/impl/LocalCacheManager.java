/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl;

import net.sf.ehcache.CacheManager;

import org.hibernate.ogm.datastore.ehcache.impl.Cache;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;

/**
 * Provides access to the Ehcache caches used for storing entities, associations and id sources. The number of caches
 * used depends on the specific implementation of this contract.
 * <p>
 * Implementations need to make sure all needed caches are started before state transfer happens. This prevents this
 * node to return undefined cache errors during replication when other nodes join this one.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Sanne Grinovero
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 * @param <EK> the entity cache key type
 * @param <AK> the association cache key type
 * @param <ISK> the identity source cache key type
 */
public abstract class LocalCacheManager<EK, AK, ISK> {

	private final CacheManager cacheManager;

	protected LocalCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	protected CacheManager getCacheManager() {
		return cacheManager;
	}

	public void stop() {
		cacheManager.shutdown();
	}

	public abstract Cache<EK> getEntityCache(EntityKeyMetadata keyMetadata);

	public abstract Cache<AK> getAssociationCache(AssociationKeyMetadata keyMetadata);

	public abstract Cache<ISK> getIdSourceCache(IdSourceKeyMetadata keyMetadata);

	/**
	 * Determines the caches storing the entities of the given key families, fetches the keys of these families and
	 * invokes the given processor for each key.
	 *
	 * @param processor the processor that will be invoked on the keys of the given families
	 * @param entityKeyMetadatas the meta-data of the keys to process
	 */
	public abstract void forEachTuple(KeyProcessor<EK> processor, EntityKeyMetadata... entityKeyMetadatas);

	/**
	 * Processes keys obtained from a cache.
	 *
	 * @param <EK> the type of the keys
	 */
	public interface KeyProcessor<EK> {
		void processKey(EK key, Cache<EK> cache);
	}
}
