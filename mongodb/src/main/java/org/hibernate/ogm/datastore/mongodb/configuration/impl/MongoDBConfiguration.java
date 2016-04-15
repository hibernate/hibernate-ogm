/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.configuration.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;

import org.hibernate.ogm.cfg.spi.DocumentStoreConfiguration;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.options.AuthenticationMechanismType;
import org.hibernate.ogm.datastore.mongodb.options.impl.ReadPreferenceOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernOption;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Configuration for {@link MongoDBDatastoreProvider}.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 * @author Hardy Ferentschik
 */
public class MongoDBConfiguration extends DocumentStoreConfiguration {

	public static final String DEFAULT_ASSOCIATION_STORE = "Associations";

	private static final int DEFAULT_PORT = 27017;
	private static final Log log = LoggerFactory.getLogger();

	private final WriteConcern writeConcern;
	private final ReadPreference readPreference;
	private final AuthenticationMechanismType authenticationMechanism;
	private final ConfigurationPropertyReader propertyReader;

	/**
	 * Creates a new {@link MongoDBConfiguration}.
	 *
	 * @param propertyReader provides access to configuration values given via {@code persistence.xml} etc.
	 * @param globalOptions global settings given via an option configurator
	 */
	public MongoDBConfiguration(ConfigurationPropertyReader propertyReader, OptionsContext globalOptions) {
		super( propertyReader, DEFAULT_PORT );

		this.propertyReader = propertyReader;
		this.authenticationMechanism = propertyReader.property( MongoDBProperties.AUTHENTICATION_MECHANISM, AuthenticationMechanismType.class )
				.withDefault( AuthenticationMechanismType.BEST )
				.getValue();
		this.writeConcern = globalOptions.getUnique( WriteConcernOption.class );
		this.readPreference = globalOptions.getUnique( ReadPreferenceOption.class );
	}

	/**
	 * Create a {@link MongoClientOptions} using the {@link MongoDBConfiguration}.
	 *
	 * @return the {@link MongoClientOptions} corresponding to the {@link MongoDBConfiguration}
	 */
	public MongoClientOptions buildOptions() {
		MongoClientOptions.Builder optionsBuilder = MongoClientOptions.builder();

		optionsBuilder.writeConcern( writeConcern );
		optionsBuilder.readPreference( readPreference );

		Map<String, Method> settingsMap = createSettingsMap();
		for ( Map.Entry<String, Method> entry : settingsMap.entrySet() ) {
			String setting = MongoDBProperties.MONGO_DRIVER_SETTINGS_PREFIX + "." + entry.getKey();
			// we know that there is exactly one parameter
			Class<?> paramType = entry.getValue().getParameterTypes()[0];
			Class<?> propType;

			// for reflection purposes we need to deal with wrapper classes
			if ( int.class.equals( paramType ) ) {
				paramType = Integer.class;
				propType = paramType;
			}
			else if ( boolean.class.equals( paramType ) ) {
				paramType = Boolean.class;
				propType = paramType;
			}
			else {
				// Any other type is received as a String mentioning the builder's class
				// name or a real String value.
				propType = String.class;
			}

			Object property = propertyReader.property( setting, propType ).withDefault( null ).getValue();
			if ( property == null ) {
				continue;
			}

			// If the parameter type and the provided type do not match, it means that
			// the user passed a builder class. We must consider the value as the
			// builder's class name and call the corresponding method to retrieve the
			// actual object.
			if ( paramType != propType ) {
				if ( !( property instanceof String ) ) {
					throw log.unexpectedTypeInProperty( setting, property.getClass().getName() );
				}

				Class<?> builderClass;
				try {
					builderClass = Class.forName( (String) property );
				}
				catch ( ClassNotFoundException e ) {
					throw log.unknownUserClass( (String) property );
				}
				try {
					property = builderClass.getMethod( entry.getKey() ).invoke( null );
				}
				catch ( NoSuchMethodException | InvocationTargetException | IllegalAccessException e ) {
					throw log.unableToInvokeMethodViaReflection( (String) property, entry.getKey() );
				}
			}

			Method settingMethod = entry.getValue();
			try {
				settingMethod.invoke( optionsBuilder, property );
			}
			catch ( InvocationTargetException | IllegalAccessException e ) {
				throw log.unableToInvokeMethodViaReflection(
						settingMethod.getDeclaringClass().getName(),
						settingMethod.getName()
				);
			}
		}

		return optionsBuilder.build();
	}

	private Map<String, Method> createSettingsMap() {
		Map<String, Method> settingsMap = new HashMap<>();

		Method[] methods = MongoClientOptions.Builder.class.getDeclaredMethods();
		for ( Method method : methods ) {
			if ( method.getParameterTypes().length == 1
					&& MongoClientOptions.Builder.class.equals( method.getReturnType() ) ) {
					settingsMap.put( method.getName(), method );
			}
		}

		return settingsMap;
	}

	public List<MongoCredential> buildCredentials() {
		if ( getUsername() != null ) {
			return Collections.singletonList(
					authenticationMechanism.createCredential(
							getUsername(),
							getDatabaseName(),
							getPassword()
					)
			);
		}
		return null;
	}
}
