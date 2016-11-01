/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.configuration.impl;

import java.net.URL;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.internal.IgnitionEx;
import org.hibernate.ogm.datastore.ignite.IgniteConfigurationBuilder;
import org.hibernate.ogm.datastore.ignite.IgniteProperties;
import org.hibernate.ogm.datastore.ignite.impl.IgniteDatastoreProvider;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Configuration for {@link IgniteDatastoreProvider}.
 * @author Dmitriy Kozlov
 * @author Victor Kadachigov
 */
public class IgniteProviderConfiguration {

	private static final Log log = LoggerFactory.getLogger();

	/**
	 * Name of the default Ignite configuration file
	 */
	private static final String DEFAULT_CONFIG = "ignite-config.xml";

	private URL url;
	private String instanceName;
	private Class<IgniteConfigurationBuilder> configBuilderClass;

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

		String className = new ConfigurationPropertyReader( configurationMap )
				.property( IgniteProperties.CONFIGURATION_CLASS_NAME, String.class )
				.getValue();
		if ( StringUtils.isNotEmpty( className ) ) {
			try {
				this.configBuilderClass = (Class<IgniteConfigurationBuilder>) Class.forName( className );
			}
			catch (ClassNotFoundException ex) {
				throw log.invalidPropertyValue( IgniteProperties.CONFIGURATION_CLASS_NAME, ex.getMessage(), ex );
			}
		}

		this.instanceName = new ConfigurationPropertyReader( configurationMap )
				.property( IgniteProperties.IGNITE_INSTANCE_NAME, String.class )
				.getValue();
	}

	/**
	 * @see IgniteProperties#CONFIGURATION_RESOURCE_NAME
	 * @return An URL to Ignite configuration file
	 */
	public URL getUrl() {
		return url;
	}

	/**
	 * @see IgniteProperties#IGNITE_INSTANCE_NAME
	 * @return the name of existing Ignite instance
	 */
	public String getInstanceName() {
		return instanceName;
	}

	public IgniteConfiguration getOrCreateIgniteConfiguration() {
		IgniteConfiguration conf = null;
		if ( configBuilderClass != null ) {
			try {
				IgniteConfigurationBuilder configBuilder = configBuilderClass.newInstance();
				conf = configBuilder.build();
			}
			catch (InstantiationException | IllegalAccessException ex) {
				throw log.unableToStartDatastoreProvider( ex );
			}
		}
		if ( url != null && conf == null ) {
			try	{
				conf = IgnitionEx.loadConfiguration( url ).get1();
			}
			catch (IgniteCheckedException ex) {
				throw log.unableToStartDatastoreProvider( ex );
			}
		}
		if ( conf != null ) {
			conf.setGridName( getOrCreateGridName() );
		}
		return conf;
	}

	public String getOrCreateGridName() {
		String result = null;
		if ( StringUtils.isNotEmpty( instanceName ) ) {
			result = instanceName;
		}
		else if ( url != null ) {
			result = url.getPath();
			result = result.replaceAll( "[\\,\\\",:,\\*,\\/,\\\\]", "_" );
		}
		return result;
	}
}
