/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.keyvalue.options.navigation;

import org.hibernate.ogm.options.navigation.EntityContext;

/**
 * Allows to configure key/value store options applying on a per-entity level. These options can be overridden for
 * single properties.
 *
 * @author Gunnar Morling
 */
public interface KeyValueStoreEntityContext<E extends KeyValueStoreEntityContext<E, P>, P extends KeyValueStorePropertyContext<E, P>> extends
		EntityContext<E, P> {
}
