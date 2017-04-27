/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.options.navigation.impl;

import org.hibernate.ogm.datastore.ignite.options.navigation.IgniteEntityContext;
import org.hibernate.ogm.datastore.ignite.options.navigation.IgnitePropertyContext;
import org.hibernate.ogm.datastore.keyvalue.options.navigation.spi.BaseKeyValueStorePropertyContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Converts Ignite property-level options.
 *
 * @author Dmitriy Kozlov
 *
 */
public abstract class IgnitePropertyContextImpl extends
		BaseKeyValueStorePropertyContext<IgniteEntityContext, IgnitePropertyContext> implements IgnitePropertyContext {

	public IgnitePropertyContextImpl(ConfigurationContext context) {
		super( context );
	}

}
