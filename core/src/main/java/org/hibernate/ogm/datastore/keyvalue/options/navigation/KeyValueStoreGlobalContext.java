/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.keyvalue.options.navigation;

import org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType;
import org.hibernate.ogm.options.navigation.GlobalContext;

/**
 * Allows to configure key/value store options applying on a global level. These options may be overridden for single
 * entities or properties.
 *
 * @author Gunnar Morling
 */
public interface KeyValueStoreGlobalContext<G extends KeyValueStoreGlobalContext<G, E>, E extends KeyValueStoreEntityContext<E, ?>> extends GlobalContext<G, E> {

	/**
	 * Specifies how data (entity, associations, id source) should be mapped to caches.
	 *
	 * @param cacheMapping the cache mapping type to be used when not configured on the entity or property level
	 * @return this context, allowing for further fluent API invocations
	 */
	G cacheMapping(CacheMappingType cacheMapping);
}
