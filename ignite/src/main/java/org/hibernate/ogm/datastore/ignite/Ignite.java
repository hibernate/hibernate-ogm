package org.hibernate.ogm.datastore.ignite;

import org.hibernate.ogm.datastore.ignite.options.navigation.IgniteGlobalContext;
import org.hibernate.ogm.datastore.ignite.options.navigation.impl.IgniteEntityContextImpl;
import org.hibernate.ogm.datastore.ignite.options.navigation.impl.IgniteGlobalContextImpl;
import org.hibernate.ogm.datastore.ignite.options.navigation.impl.IgnitePropertyContextImpl;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Allows to configure options specific to the Ignite data store.
 * 
 * @author Dmitriy Kozlov
 *
 */
public class Ignite implements DatastoreConfiguration<IgniteGlobalContext>{

	@Override
	public IgniteGlobalContext getConfigurationBuilder(ConfigurationContext context) {
		return context.createGlobalContext(IgniteGlobalContextImpl.class, IgniteEntityContextImpl.class, IgnitePropertyContextImpl.class);
	}

}
