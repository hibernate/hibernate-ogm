/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.keyvalue.options.navigation.spi;

import org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType;
import org.hibernate.ogm.datastore.keyvalue.options.navigation.KeyValueStoreEntityContext;
import org.hibernate.ogm.datastore.keyvalue.options.navigation.KeyValueStoreGlobalContext;
import org.hibernate.ogm.datastore.keyvalue.options.spi.CacheMappingOption;
import org.hibernate.ogm.options.navigation.spi.BaseGlobalContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Converts global key/value store options.
 *
 * @author Gunnar Morling
 */
public abstract class BaseKeyValueStoreGlobalContext<G extends KeyValueStoreGlobalContext<G, E>, E extends KeyValueStoreEntityContext<E, ?>> extends
		BaseGlobalContext<G, E> implements KeyValueStoreGlobalContext<G, E> {

	public BaseKeyValueStoreGlobalContext(ConfigurationContext context) {
		super( context );
	}

	@Override
	public G cacheMapping(CacheMappingType cacheMapping) {
		addGlobalOption( new CacheMappingOption(), cacheMapping );

		// ok; an error would only occur for inconsistently defined context types
		@SuppressWarnings("unchecked")
		G context = (G) this;
		return context;
	}
}
