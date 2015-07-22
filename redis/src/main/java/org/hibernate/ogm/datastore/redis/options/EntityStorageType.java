/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.options;

/**
 * Strategies for storing entities in Redis.
 *
 * @author Mark Paluch
 */
public enum EntityStorageType {

	/**
	 * Store the entity as JSON in a global string value.
	 */
	JSON,

	/**
	 * Stores the entity in a hash and store all fields as strings.
	 */
	HASH
}
