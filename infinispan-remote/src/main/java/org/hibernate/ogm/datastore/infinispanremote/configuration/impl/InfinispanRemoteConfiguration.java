/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.configuration.impl;

import static org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties.HOT_ROD_CLIENT_PREFIX;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaCapture;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaOverride;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Configuration for {@link InfinispanRemoteProperties}.
 */
public class InfinispanRemoteConfiguration {

	private static final Log log = LoggerFactory.getLogger();

	private URL configurationResource;

	private SchemaCapture schemaCaptureService;

	private SchemaOverride schemaOverrideService;

	private String schemaPackageName;

	private Properties clientProperties;

	/**
	 * The location of the configuration file.
	 *
	 * @see InfinispanRemoteProperties#CONFIGURATION_RESOURCE_NAME
	 * @return might be the name of the file (too look it up in the class path) or an URL to a file.
	 */
	public URL getConfigurationResourceUrl() {
		return configurationResource;
	}

	/**
	 * Extract from the configuration the properties to apply to the Hot Rod (Infinispan remote) client.
	 *
	 * @return the clientProperties Hot Rod client properties
	 */
	public Properties getClientProperties() {
		return clientProperties;
	}

	public SchemaCapture getSchemaCaptureService() {
		return schemaCaptureService;
	}

	public SchemaOverride getSchemaOverrideService() {
		return schemaOverrideService;
	}

	public String getSchemaPackageName() {
		return schemaPackageName;
	}

	/**
	 * Initialize the internal values form the given {@link Map}.
	 *
	 * @param configurationMap
	 *            The values to use as configuration
	 * @param serviceRegistry
	 */
	public void initConfiguration(Map<?, ?> configurationMap, ServiceRegistryImplementor serviceRegistry) {
		ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationMap, classLoaderService );

		this.configurationResource = propertyReader
				.property( InfinispanRemoteProperties.CONFIGURATION_RESOURCE_NAME, URL.class )
				.getValue();

		this.clientProperties = getHotRodConfiguration( configurationMap, propertyReader, this.configurationResource );

		this.schemaCaptureService = propertyReader
				.property( InfinispanRemoteProperties.SCHEMA_CAPTURE_SERVICE, SchemaCapture.class )
				.instantiate()
				.getValue();

		this.schemaOverrideService = propertyReader
				.property( InfinispanRemoteProperties.SCHEMA_OVERRIDE_SERVICE, SchemaOverride.class )
				.instantiate()
				.getValue();

		this.schemaPackageName = propertyReader
				.property( InfinispanRemoteProperties.SCHEMA_PACKAGE_NAME, String.class )
				.withDefault( InfinispanRemoteProperties.DEFAULT_SCHEMA_PACKAGE_NAME )
				.getValue();

		log.tracef( "Initializing Infinispan Hot Rod client from configuration file at '%1$s'", configurationResource );
	}

	/**
	 * Extract from the configuration the Hot Rod client properties: the one prefixed with
	 * {@link InfinispanRemoteProperties#HOT_ROD_CLIENT_PREFIX} or defined in the resource file via
	 * {@link InfinispanRemoteProperties#CONFIGURATION_RESOURCE_NAME}.
	 * <p>
	 * Note that the properties with the prefix will override the same properties in the resource file.
	 */
	private Properties getHotRodConfiguration(Map<?, ?> configurationMap, ConfigurationPropertyReader propertyReader, URL configurationResourceUrl) {
		Properties hotRodConfiguration = new Properties();
		if ( configurationResourceUrl != null ) {
			try ( InputStream openStream = configurationResourceUrl.openStream() ) {
				hotRodConfiguration.load( openStream );
			}
			catch (IOException e) {
				throw log.failedLoadingHotRodConfigurationProperties( e );
			}
		}
		// Programmatic properties override the resource file
		for ( Entry<?, ?> property : configurationMap.entrySet() ) {
			String key = (String) property.getKey();
			if ( key.startsWith( HOT_ROD_CLIENT_PREFIX ) ) {
				String value = propertyReader.property( key, String.class ).withDefault( null ).getValue();
				String setting = key.substring( HOT_ROD_CLIENT_PREFIX.length() );
				hotRodConfiguration.setProperty( setting, value );
			}
		}
		if ( hotRodConfiguration.isEmpty() ) {
			throw log.hotrodClientConfigurationMissing();
		}
		return hotRodConfiguration;
	}
}
