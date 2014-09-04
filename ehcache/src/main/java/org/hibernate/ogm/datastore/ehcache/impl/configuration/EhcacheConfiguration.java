/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.impl.configuration;

import java.net.URL;
import java.util.Map;

import org.hibernate.ogm.datastore.ehcache.EhcacheProperties;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Configuration for {@link org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider}.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
public class EhcacheConfiguration {

	/**
	 * Name of the default Ehcache configuration file
	 */
	private static final String DEFAULT_CONFIG = "org/hibernate/ogm/datastore/ehcache/default-ehcache.xml";

	private URL url;

	/**
	 * Initialize the internal values from the given {@link Map}.
	 *
	 * @param configurationMap The values to use as configuration
	 */
	public void initialize(Map configurationMap) {
		this.url = new ConfigurationPropertyReader( configurationMap )
			.property( EhcacheProperties.CONFIGURATION_RESOURCE_NAME, URL.class )
			.withDefault( EhcacheConfiguration.class.getClassLoader().getResource( DEFAULT_CONFIG ) )
			.getValue();
	}

	/**
	 * @see EhcacheProperties#CONFIGURATION_RESOURCE_NAME
	 * @return An URL to an XML file compliant with the ehcache.xsd schema.
	 */
	public URL getUrl() {
		return url;
	}
}
