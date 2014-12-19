/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.configuration.impl;

import java.util.Arrays;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.spi.DocumentStoreConfiguration;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.options.impl.ReadPreferenceOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernOption;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.ogm.util.configurationreader.spi.PropertyValidator;

import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;

/**
 * Configuration for {@link MongoDBDatastoreProvider}.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class MongoDBConfiguration extends DocumentStoreConfiguration {

	public static final String DEFAULT_ASSOCIATION_STORE = "Associations";

	/**
	 * The default value used to set the timeout during the connection to the MongoDB instance This value is set in
	 * milliseconds.
	 *
	 * @see MongoDBProperties#TIMEOUT
	 */
	private static final int DEFAULT_TIMEOUT = 5000;

	private static final int DEFAULT_PORT = 27017;

	private static final Log log = LoggerFactory.getLogger();

	private static final TimeoutValidator TIMEOUT_VALIDATOR = new TimeoutValidator();

	private final int timeout;
	private final WriteConcern writeConcern;
	private final ReadPreference readPreference;
	private final String authenticationMechanism;

	/**
	 * Creates a new {@link MongoDBConfiguration}.
	 *
	 * @param propertyReader provides access to configuration values given via {@code persistence.xml} etc.
	 * @param globalOptions global settings given via an option configurator
	 */
	public MongoDBConfiguration(ConfigurationPropertyReader propertyReader, OptionsContext globalOptions) {
		super( propertyReader, DEFAULT_PORT );

		this.timeout = propertyReader.property( MongoDBProperties.TIMEOUT, int.class )
				.withDefault( DEFAULT_TIMEOUT )
				.withValidator( TIMEOUT_VALIDATOR )
				.getValue();
		this.authenticationMechanism = propertyReader.property( MongoDBProperties.AUTHENTICATION_MECHANISM, String.class )
				.withDefault( MongoCredential.MONGODB_CR_MECHANISM )
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
		MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder();

		optionsBuilder.connectTimeout( timeout );
		optionsBuilder.writeConcern( writeConcern );
		optionsBuilder.readPreference( readPreference );
		return optionsBuilder.build();
	}

	public List<MongoCredential> buildCredentials() {
		if ( getUsername() != null ) {
			if ( MongoCredential.MONGODB_CR_MECHANISM.equals( authenticationMechanism ) ) {
				return Arrays.asList( MongoCredential.createMongoCRCredential( getUsername(), getDatabaseName(), password() ) );
			}
			if ( MongoCredential.GSSAPI_MECHANISM.equals( authenticationMechanism ) ) {
				return Arrays.asList( MongoCredential.createGSSAPICredential( getUsername() ) );
			}
			if ( MongoCredential.MONGODB_X509_MECHANISM.equals( authenticationMechanism ) ) {
				return Arrays.asList( MongoCredential.createMongoX509Credential( getUsername() ) );
			}
			if ( MongoCredential.PLAIN_MECHANISM.equals( authenticationMechanism ) ) {
				return Arrays.asList( MongoCredential.createPlainCredential( getUsername(), getDatabaseName(), password() ) );
			}
			throw log.authenticationMechanismNotRecognized( authenticationMechanism );
		}
		return null;
	}

	private char[] password() {
		if ( getPassword() == null ) {
			return null;
		}
		return getPassword().toCharArray();
	}

	private static class TimeoutValidator implements PropertyValidator<Integer> {

		@Override
		public void validate(Integer value) throws HibernateException {
			if ( value < 0 ) {
				throw log.mongoDBTimeOutIllegalValue( value );
			}
		}
	}
}
