/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.configuration.impl;

import static org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties.HOT_ROD_CLIENT_PREFIX;
import static org.infinispan.client.hotrod.impl.ConfigurationProperties.FORCE_RETURN_VALUES;
import static org.infinispan.client.hotrod.impl.ConfigurationProperties.MARSHALLER;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.OgmProtoStreamMarshaller;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaCapture;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaOverride;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;

/**
 * Configuration for {@link InfinispanRemoteProperties}.
 * <p>
 * This class also keep track of the configuration for the Hot Rod client.
 * There are two ways to configure the client:
 * <ul>
 *   <li>Using an external Hot Rod properties file; see {@link InfinispanRemoteProperties#CONFIGURATION_RESOURCE_NAME}
 *   <li>Defining the properties of the client using {@value InfinispanRemoteProperties#HOT_ROD_CLIENT_PREFIX}
 * </ul>
 * <p>
 * When a property of the Hot Rod client is set in the hibernate configuration, it should be prefixed
 * with {@value InfinispanRemoteProperties#HOT_ROD_CLIENT_PREFIX}.
 * For example, the property {@code infinispan.client.hotrod.server_list}
 * becomes {@code hibernate.ogm.infinispan_remote.client.server_list}.
 * <p>
 * Currently, some properties in the Hot Rod client don't have a prefix, in this case it must be added
 * in the hibernate configuration.
 * For example, the property {@code maxActive} becomes {@code hibernate.ogm.infinispan_remote.client.maxActive}
 * <p>
 * Properties with the Hibernate OGM prefix ({@value InfinispanRemoteProperties#HOT_ROD_CLIENT_PREFIX}) will
 * override corresponding properties defined in the external Hot Rod configuration file.
 *
 * @see InfinispanRemoteProperties
 * @see ConfigurationProperties
 *
 * @author Davide D'Alto
 */
public class InfinispanRemoteConfiguration {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	/**
	 * The prefix used by many configuration properties for Hot Rod
	 */
	private static final String HOT_ROD_ORIGINAL_PREFIX = "infinispan.client.hotrod.";

	/*
	 * Currently, some properties in Hot Rod are without a prefix
	 */
	private static String[] noPrefixProperties = {
		"exhaustedAction",
		"maxActive",
		"maxTotal",
		"maxWait",
		"maxIdle",
		"minIdle",
		"numTestsPerEvictionRun",
		"minEvictableIdleTimeMillis",
		"timeBetweenEvictionRunsMillis",
		"lifo",
		"testOnBorrow",
		"testOnReturn",
		"testWhileIdle"
	};

	/*
	 * The expected configuration value for some properties, an exception is thrown if the value is changed.
	 */
	private static final String[][] expectedValuesForHotRod = {
			{ FORCE_RETURN_VALUES, "true" },
			{ MARSHALLER, OgmProtoStreamMarshaller.class.getName() }
	};

	private URL configurationResource;

	private SchemaCapture schemaCaptureService;

	private SchemaOverride schemaOverrideService;

	private String schemaPackageName;

	private Properties clientProperties;

	private boolean createCachesEnabled;

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

	public boolean isCreateCachesEnabled() {
		return createCachesEnabled;
	}

	/**
	 * Initialize the internal values from the given {@link Map}.
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

		this.createCachesEnabled = propertyReader
				.property( OgmProperties.CREATE_DATABASE, boolean.class )
				.withDefault( false )
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
		loadResourceFile( configurationResourceUrl, hotRodConfiguration );
		setAdditionalProperties( configurationMap, propertyReader, hotRodConfiguration );
		setExpectedPropertiesIfNull( hotRodConfiguration );
		validate( hotRodConfiguration );
		return hotRodConfiguration;
	}

	/**
	 * Load the properties from the resource file if one is specified
	 */
	private void loadResourceFile(URL configurationResourceUrl, Properties hotRodConfiguration) {
		if ( configurationResourceUrl != null ) {
			try ( InputStream openStream = configurationResourceUrl.openStream() ) {
				hotRodConfiguration.load( openStream );
			}
			catch (IOException e) {
				throw log.failedLoadingHotRodConfigurationProperties( e );
			}
		}
	}

	/**
	 * Set the properties defined using the prefix {@link InfinispanRemoteProperties#HOT_ROD_CLIENT_PREFIX}
	 *
	 * @param configurationMap contains all the properties defined for OGM
	 * @param propertyReader read the value of a property
	 * @param hotRodConfiguration the Hot Rod configuration to update
	 */
	private void setAdditionalProperties(Map<?, ?> configurationMap, ConfigurationPropertyReader propertyReader, Properties hotRodConfiguration) {
		// Programmatic properties override the resource file
		for ( Entry<?, ?> property : configurationMap.entrySet() ) {
			String key = (String) property.getKey();
			if ( key.startsWith( HOT_ROD_CLIENT_PREFIX ) ) {
				String hotRodProperty = key.substring( HOT_ROD_CLIENT_PREFIX.length() );
				String value = propertyReader.property( key, String.class ).getValue();
				if ( !ArrayHelper.contains( noPrefixProperties, hotRodProperty ) ) {
					hotRodProperty = HOT_ROD_ORIGINAL_PREFIX + hotRodProperty;
				}
				hotRodConfiguration.setProperty( hotRodProperty, value );
			}
		}
	}

	/*
	 * We provide some default values in case some properties are not set
	 */
	private void setExpectedPropertiesIfNull(Properties hotRodConfiguration) {
		for ( int i = 0; i < expectedValuesForHotRod.length; i++ ) {
			String property = expectedValuesForHotRod[i][0];
			String expectedValue = expectedValuesForHotRod[i][1];
			if ( !hotRodConfiguration.containsKey( property ) ) {
				hotRodConfiguration.setProperty( property, expectedValue );
			}
		}
	}

	private void validate(Properties hotRodConfiguration) {
		for ( int i = 0; i < expectedValuesForHotRod.length; i++ ) {
			String property = expectedValuesForHotRod[i][0];
			String expectedValue = expectedValuesForHotRod[i][1];
			String actualValue = trim( hotRodConfiguration.getProperty( property ) );
			if ( !expectedValue.equals( actualValue ) && !expectedValue.equalsIgnoreCase( actualValue ) ) {
				throw log.invalidConfigurationValue( property, expectedValue, actualValue );
			}
		}
	}

	private String trim(String property) {
		if ( property == null ) {
			return null;
		}
		return property.trim();
	}
}
