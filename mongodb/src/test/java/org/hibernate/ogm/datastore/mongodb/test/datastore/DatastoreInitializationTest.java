/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.datastore;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.configuration.impl.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.options.AuthenticationMechanismType;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.utils.SkippableTestRunner;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.service.spi.ServiceException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Hardy Ferentschik
 */
@RunWith(SkippableTestRunner.class)
public class DatastoreInitializationTest {

	/**
	 * The IP address 203.0.113.1 has been chosen because of: "203.0.113.0/24 No Assigned as "TEST-NET-3" in RFC 5737
	 * for use solely in documentation and example source code and should not be used publicly."
	 */
	private static final String NON_EXISTENT_IP = "203.0.113.1";

	@Rule
	public ExpectedException error = ExpectedException.none();

	private Map<String, Object> cfg;

	@Before
	public void setUp() {
		cfg =  new HashMap<String, Object>( TestHelper.getDefaultTestSettings() );
		cfg.put( OgmProperties.DATABASE, "snafu" );
		cfg.put( OgmProperties.USERNAME, "foo" );
		cfg.put( OgmProperties.PASSWORD, "bar" );
		cfg.put( "hibernate.ogm.mongodb.driver.serverSelectionTimeout", "1000" );
	}

	@Test
	public void testAuthentication() throws Exception {
		error.expect( ServiceException.class );
		error.expectMessage( "OGM000071" );
		// the timeout exception thrown by the driver will actually contain some information about the authentication
		// error. Obviously quite fragile. Might change
		error.expectCause( hasMessage( containsString( "Exception authenticating" ) ) );

		StandardServiceRegistry serviceRegistry = TestHelper.getDefaultTestStandardServiceRegistry( cfg );

		// will start the service
		serviceRegistry.getService( DatastoreProvider.class );
	}

	@Test
	public void testDefaultAuthenticationMechanism() throws Exception {
		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();
		cfg.put( OgmProperties.DATASTORE_PROVIDER, provider );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );

		assertThat( provider.leakingClient.getCredentialsList().get( 0 ).getMechanism() ).isEqualTo( null );
	}

	@Test
	public void testSCRAMSHA1AuthenticationMechanism() throws Exception {
		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, AuthenticationMechanismType.SCRAM_SHA_1.name() );
		cfg.put( OgmProperties.DATASTORE_PROVIDER, provider );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );

		assertThat(
				provider.leakingClient.getCredentialsList()
						.get( 0 )
						.getMechanism()
		).isEqualTo( MongoCredential.SCRAM_SHA_1_MECHANISM );
	}

	@Test
	public void testX509AuthenticationMechanism() throws Exception {
		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();

		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, AuthenticationMechanismType.MONGODB_X509.name() );
		cfg.put( OgmProperties.DATASTORE_PROVIDER, provider );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );

		assertThat(
				provider.leakingClient.getCredentialsList()
						.get( 0 )
						.getMechanism()
		).isEqualTo( MongoCredential.MONGODB_X509_MECHANISM );
	}

	@Test
	public void testGSSAPIAuthenticationMechanism() throws Exception {
		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, AuthenticationMechanismType.GSSAPI.name() );
		cfg.put( OgmProperties.DATASTORE_PROVIDER, provider );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );

		assertThat(
				provider.leakingClient.getCredentialsList()
						.get( 0 )
						.getMechanism()
		).isEqualTo( MongoCredential.GSSAPI_MECHANISM );
	}

	@Test
	public void testPlainAuthenticationMechanism() throws Exception {
		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, AuthenticationMechanismType.PLAIN.name() );
		cfg.put( OgmProperties.DATASTORE_PROVIDER, provider );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );

		assertThat(
				provider.leakingClient.getCredentialsList()
						.get( 0 )
						.getMechanism()
		).isEqualTo( MongoCredential.PLAIN_MECHANISM );
	}

	@Test
	public void testNotRecognizedAuthenticationMechanism() throws Exception {
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, "alhdfoiehfnl" );

		error.expect( ServiceException.class );
		error.expectMessage( "OGM000072" );
		//nested exception
		error.expectCause( hasMessage( containsString( "OGM000051" ) ) );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );
	}

	@Test
	public void testConnectionErrorWrappedInHibernateException() throws Exception {
		cfg.put( OgmProperties.HOST, NON_EXISTENT_IP );

		error.expect( ServiceException.class );
		error.expectMessage( "OGM000071" );
		//nested exception
		error.expectCause( hasMessage( containsString( "OGM001214" ) ) );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );
	}

	@Test
	public void testConnectionTimeout() {
		cfg.put( OgmProperties.HOST, NON_EXISTENT_IP );
		cfg.put( OgmProperties.DATABASE, "ogm_test_database" );

		error.expect( ServiceException.class );
		error.expectMessage( "OGM000071" );
		// the timeout exception thrown by the driver will actually contain some information about the authentication
		// error. Obviously quite fragile. Might change
		error.expectCause( hasMessage( containsString( "Timed out" ) ) );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );
	}

	class LeakingMongoDBDatastoreProvider extends MongoDBDatastoreProvider {

		public MongoClient leakingClient;

		@Override
		protected MongoClient createMongoClient(MongoDBConfiguration config) {
			MongoClient mongoClient = super.createMongoClient( config );
			this.leakingClient = mongoClient;
			return mongoClient;
		}

		@Override
		public void start() {
			try {
				super.start();
			}
			catch ( Exception e ) {
			}
		}
	}
}
