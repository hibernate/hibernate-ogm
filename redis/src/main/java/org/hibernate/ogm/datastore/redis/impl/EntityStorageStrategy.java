/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import org.hibernate.ogm.datastore.redis.dialect.value.Entity;

/**
 * A strategy abstraction for how entities are persisted and loaded in Redis.
 *
 * @author Mark Paluch
 */
public interface EntityStorageStrategy {

	/**
	 * Persiste an entity to Redis.
	 *
	 * @param key the key
	 *
	 * @return the entity or {@literal null}
	 */
	Entity getEntity(byte[] key);

	/**
	 * Store an entity
	 *
	 * @param key the key
	 * @param entity the entity
	 */
	void storeEntity(byte[] key, Entity entity);
}
