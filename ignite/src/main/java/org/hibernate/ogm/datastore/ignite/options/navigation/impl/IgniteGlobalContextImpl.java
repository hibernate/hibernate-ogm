package org.hibernate.ogm.datastore.ignite.options.navigation.impl;

import org.hibernate.ogm.datastore.ignite.options.navigation.IgniteEntityContext;
import org.hibernate.ogm.datastore.ignite.options.navigation.IgniteGlobalContext;
import org.hibernate.ogm.datastore.keyvalue.options.navigation.spi.BaseKeyValueStoreGlobalContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Converts global Ignite options.
 * 
 * @author Dmitriy Kozlov
 *
 */
public abstract class IgniteGlobalContextImpl extends BaseKeyValueStoreGlobalContext<IgniteGlobalContext, IgniteEntityContext> 
implements IgniteGlobalContext {

	public IgniteGlobalContextImpl(ConfigurationContext context) {
		super(context);
	}

}
