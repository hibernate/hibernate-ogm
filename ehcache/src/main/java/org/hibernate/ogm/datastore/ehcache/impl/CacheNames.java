/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.impl;

/**
 * The names of the caches used to store entities etc. in Ehcache.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
final class CacheNames {
	static final String ENTITY_CACHE = "ENTITIES";
	static final String ASSOCIATION_CACHE = "ASSOCIATIONS";
	static final String IDENTIFIER_CACHE = "IDENTIFIERS";
}
