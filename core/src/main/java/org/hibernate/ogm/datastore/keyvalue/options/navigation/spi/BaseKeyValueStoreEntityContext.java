/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.keyvalue.options.navigation.spi;

import org.hibernate.ogm.datastore.keyvalue.options.navigation.KeyValueStoreEntityContext;
import org.hibernate.ogm.datastore.keyvalue.options.navigation.KeyValueStorePropertyContext;
import org.hibernate.ogm.options.navigation.spi.BaseEntityContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Converts key/value store entity-level options.
 *
 * @author Gunnar Morling
 */
public abstract class BaseKeyValueStoreEntityContext<E extends KeyValueStoreEntityContext<E, P>, P extends KeyValueStorePropertyContext<E, P>> extends
		BaseEntityContext<E, P> implements KeyValueStoreEntityContext<E, P> {

	public BaseKeyValueStoreEntityContext(ConfigurationContext context) {
		super( context );
	}
}
