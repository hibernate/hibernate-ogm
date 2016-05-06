/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.configuration.impl;

import java.net.URL;
import java.util.Map;

import org.hibernate.ogm.datastore.ignite.IgniteProperties;
import org.hibernate.ogm.datastore.ignite.impl.IgniteDatastoreProvider;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Configuration for {@link IgniteDatastoreProvider}.
 *
 * @author Dmitriy Kozlov
 */
public class IgniteProviderConfiguration {

	/**
	 * Name of the default Ignite configuration file
	 */
	private static final String DEFAULT_CONFIG = "org/hibernate/ogm/datastore/ignite/config.xml";

	protected URL url;

	/**
	 * Initialize the internal values from the given {@link Map}.
	 *
	 * @param configurationMap The values to use as configuration
	 */
	public void initialize(Map configurationMap) {
		this.url = new ConfigurationPropertyReader( configurationMap )
			.property( IgniteProperties.CONFIGURATION_RESOURCE_NAME, URL.class )
			.withDefault( IgniteProviderConfiguration.class.getClassLoader().getResource( DEFAULT_CONFIG ) )
			.getValue();
	}

	/**
	 * @see IgniteProperties#CONFIGURATION_RESOURCE_NAME
	 * @return An URL to an XML file
	 */
	public URL getUrl() {
		return url;
	}
}
