package org.hibernate.ogm.datastore.ignite;

import org.hibernate.ogm.datastore.keyvalue.cfg.KeyValueStoreProperties;

/**
 * Properties for configuring the Ignite datastore
 * 
 * @author Dmitriy Kozlov
 *
 */
public final class IgniteProperties implements KeyValueStoreProperties {

	/**
	 * Configuration property for specifying the name of the Ehcache configuration file
	 */
	public static final String CONFIGURATION_RESOURCE_NAME = "hibernate.ogm.ignite.configuration_resource_name";

	private IgniteProperties(){
		
	}
	
}
