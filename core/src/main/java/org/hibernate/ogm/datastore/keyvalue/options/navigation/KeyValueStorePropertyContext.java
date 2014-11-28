/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.keyvalue.options.navigation;

import org.hibernate.ogm.options.navigation.PropertyContext;

/**
 * Allows to configure key/value store options applying on a per-association level.
 *
 * @author Gunnar Morling
 */
public interface KeyValueStorePropertyContext<E extends KeyValueStoreEntityContext<E, P>, P extends KeyValueStorePropertyContext<E, P>> extends
		PropertyContext<E, P> {
}
