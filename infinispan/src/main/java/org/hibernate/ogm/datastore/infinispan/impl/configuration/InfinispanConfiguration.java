/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.impl.configuration;

import java.net.URL;
import java.util.Map;

import org.hibernate.ogm.datastore.infinispan.InfinispanProperties;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Configuration for {@link InfinispanDatastoreProvider}.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
public class InfinispanConfiguration {

	private static final Log log = LoggerFactory.make();

	private static final String INFINISPAN_DEFAULT_CONFIG = "org/hibernate/ogm/datastore/infinispan/default-config.xml";

	private URL configUrl;
	private String jndi;

	/**
	 * @see InfinispanProperties#CONFIGURATION_RESOURCE_NAME
	 * @return might be the name of the file (too look it up in the class path) or an URL to a file.
	 */
	public URL getConfigurationUrl() {
		return configUrl;
	}

	/**
	 * @see InfinispanProperties#CACHE_MANAGER_JNDI_NAME
	 * @return the {@literal JNDI} name of the cache manager
	 */
	public String getJndiName() {
		return jndi;
	}

	/**
	 * Initialize the internal values form the given {@link Map}.
	 *
	 * @param configurationMap
	 *            The values to use as configuration
	 */
	public void initConfiguration(Map configurationMap) {
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationMap );

		this.configUrl = propertyReader
				.property( InfinispanProperties.CONFIGURATION_RESOURCE_NAME, URL.class )
				.withDefault( InfinispanConfiguration.class.getClassLoader().getResource( INFINISPAN_DEFAULT_CONFIG ) )
				.getValue();

		this.jndi = propertyReader
				.property( InfinispanProperties.CACHE_MANAGER_JNDI_NAME, String.class )
				.getValue();

		log.tracef( "Initializing Infinispan from configuration file at %1$s", configUrl );
	}
}
