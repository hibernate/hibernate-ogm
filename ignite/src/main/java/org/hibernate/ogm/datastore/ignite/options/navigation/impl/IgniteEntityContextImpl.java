package org.hibernate.ogm.datastore.ignite.options.navigation.impl;

import org.hibernate.ogm.datastore.ignite.options.navigation.IgniteEntityContext;
import org.hibernate.ogm.datastore.ignite.options.navigation.IgnitePropertyContext;
import org.hibernate.ogm.datastore.keyvalue.options.navigation.spi.BaseKeyValueStoreEntityContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Converts Ignite entity-level options.
 * 
 * @author Dmitriy Kozlov
 *
 */
public abstract class IgniteEntityContextImpl extends 
		BaseKeyValueStoreEntityContext<IgniteEntityContext, IgnitePropertyContext> implements IgniteEntityContext {

	public IgniteEntityContextImpl(ConfigurationContext context) {
		super(context);
	}

}
