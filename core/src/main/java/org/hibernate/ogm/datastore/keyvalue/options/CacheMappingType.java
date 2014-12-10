/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.keyvalue.options;

/**
 * A strategy for persisting data into key/value stores.
 *
 * @author Gunnar Morling
 */
public enum CacheMappingType {

	/**
	 * Three caches will be used: one cache for all entities, one cache for all associations and one cache for all id
	 * sources.
	 */
	CACHE_PER_KIND,

	/**
	 * A dedicated cache will be used for each entity type, association type and id source table.
	 */
	CACHE_PER_TABLE;
}
