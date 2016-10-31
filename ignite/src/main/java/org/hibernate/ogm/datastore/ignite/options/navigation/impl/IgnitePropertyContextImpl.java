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
		super(context);
	}

}
