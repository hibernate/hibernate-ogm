/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import java.util.Set;

import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.model.spi.TupleOperation;

/**
 * A strategy abstraction for how entities are persisted and loaded in Redis.
 *
 * @author Mark Paluch
 */
public interface EntityStorageStrategy {

	/**
	 * Persist an entity to Redis.
	 *
	 * @param key the key, must not be {@literal null}
	 *
	 * @return the entity or {@literal null}
	 */
	Entity getEntity(byte[] key);

	/**
	 * Store an entity
	 *  @param key the key, must not be {@literal null}
	 * @param entity the entity, must not be {@literal null}
	 * @param operations tuple operations, may be {@literal null}
	 */
	void storeEntity(byte[] key, Entity entity, Set<TupleOperation> operations);
}
