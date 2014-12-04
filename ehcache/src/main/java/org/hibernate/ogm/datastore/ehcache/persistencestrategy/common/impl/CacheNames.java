/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.persistencestrategy.common.impl;

/**
 * The names of the caches used to store entities etc. in Ehcache when working with the "cache-per-kind" strategy.
 * <p>
 * These caches will also be used as "template" for the caches of the respective kinds when using the "cache-per-table"
 * strategy, unless a cache for a given table has been configured explicitly.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public final class CacheNames {

	public static final String ENTITY_CACHE = "ENTITIES";
	public static final String ASSOCIATION_CACHE = "ASSOCIATIONS";
	public static final String IDENTIFIER_CACHE = "IDENTIFIERS";
}
