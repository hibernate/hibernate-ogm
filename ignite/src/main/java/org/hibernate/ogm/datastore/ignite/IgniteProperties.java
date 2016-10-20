/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
	 * Configuration property for specifying the name of the Ignite configuration file
	 */
	public static final String CONFIGURATION_RESOURCE_NAME = "hibernate.ogm.ignite.configuration_resource_name";
	/**
	 * Configuration property for specifying class name. Class must implements {@link IgniteConfigurationBuilder}
	 */
	public static final String CONFIGURATION_CLASS_NAME = "hibernate.ogm.ignite.configuration_class_name";
	/**
	 * Configuration property for specifying the name existing Ignite instance
	 */
	public static final String IGNITE_INSTANCE_NAME = "hibernate.ogm.ignite.instance_name";

	private IgniteProperties() {

	}

}
