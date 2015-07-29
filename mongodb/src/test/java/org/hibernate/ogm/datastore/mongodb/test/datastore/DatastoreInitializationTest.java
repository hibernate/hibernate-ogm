/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.datastore;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.fail;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.configuration.impl.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.options.AuthenticationMechanismType;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.utils.SkipByDatastoreProvider;
import org.hibernate.ogm.utils.SkippableTestRunner;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.service.spi.ServiceException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
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

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testAuthentication() throws Exception {
		Map<String, Object> cfg = new HashMap<String, Object>( TestHelper.getDefaultTestSettings() );
		cfg.put( OgmProperties.DATABASE, "test" );
		cfg.put( OgmProperties.USERNAME, "notauser" );
		cfg.put( OgmProperties.PASSWORD, "test" );

		StandardServiceRegistry serviceRegistry = TestHelper.getDefaultTestStandardServiceRegistry( cfg );

		error.expect( ServiceException.class );
		error.expectMessage( "OGM000071" );
		//nested exception
		error.expectCause( hasMessage( containsString( "OGM001213" ) ) );

		// will start the service
		serviceRegistry.getService( DatastoreProvider.class );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testDefaultAuthenticationMechanism() throws Exception {
		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();

		Map<String, Object> cfg = new HashMap<String, Object>( TestHelper.getDefaultTestSettings() );
		cfg.put( OgmProperties.DATABASE, "test" );
		cfg.put( OgmProperties.USERNAME, "notauser" );
		cfg.put( OgmProperties.PASSWORD, "test" );
		cfg.put( OgmProperties.DATASTORE_PROVIDER, provider );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );

		assertThat( provider.leakingClient.getCredentialsList().get( 0 ).getMechanism() ).isEqualTo( null );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testSCRAMSHA1AuthenticationMechanism() throws Exception {
		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();

		Map<String, Object> cfg = new HashMap<String, Object>( TestHelper.getDefaultTestSettings() );
		cfg.put( OgmProperties.DATABASE, "test" );
		cfg.put( OgmProperties.USERNAME, "notauser" );
		cfg.put( OgmProperties.PASSWORD, "test" );
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, AuthenticationMechanismType.SCRAM_SHA_1.name() );
		cfg.put( OgmProperties.DATASTORE_PROVIDER, provider );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );

		assertThat( provider.leakingClient.getCredentialsList().get( 0 ).getMechanism() ).isEqualTo( MongoCredential.SCRAM_SHA_1_MECHANISM );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testX509AuthenticationMechanism() throws Exception {
		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();

		Map<String, Object> cfg = new HashMap<String, Object>( TestHelper.getDefaultTestSettings() );
		cfg.put( OgmProperties.DATABASE, "test" );
		cfg.put( OgmProperties.USERNAME, "notauser" );
		cfg.put( OgmProperties.PASSWORD, "test" );
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, AuthenticationMechanismType.MONGODB_X509.name() );
		cfg.put( OgmProperties.DATASTORE_PROVIDER, provider );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );

		assertThat( provider.leakingClient.getCredentialsList().get( 0 ).getMechanism() ).isEqualTo( MongoCredential.MONGODB_X509_MECHANISM );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testGSSAPIAuthenticationMechanism() throws Exception {
		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();

		Map<String, Object> cfg = new HashMap<String, Object>( TestHelper.getDefaultTestSettings() );
		cfg.put( OgmProperties.DATABASE, "test" );
		cfg.put( OgmProperties.USERNAME, "notauser" );
		cfg.put( OgmProperties.PASSWORD, "test" );
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, AuthenticationMechanismType.GSSAPI.name() );
		cfg.put( OgmProperties.DATASTORE_PROVIDER, provider );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );

		assertThat( provider.leakingClient.getCredentialsList().get( 0 ).getMechanism() ).isEqualTo( MongoCredential.GSSAPI_MECHANISM );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testPlainAuthenticationMechanism() throws Exception {
		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();

		Map<String, Object> cfg = new HashMap<String, Object>( TestHelper.getDefaultTestSettings() );
		cfg.put( OgmProperties.DATABASE, "test" );
		cfg.put( OgmProperties.USERNAME, "notauser" );
		cfg.put( OgmProperties.PASSWORD, "test" );
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, AuthenticationMechanismType.PLAIN.name() );
		cfg.put( OgmProperties.DATASTORE_PROVIDER, provider );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );

		assertThat( provider.leakingClient.getCredentialsList().get( 0 ).getMechanism() ).isEqualTo( MongoCredential.PLAIN_MECHANISM );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testNotRecognizedAuthenticationMechanism() throws Exception {
		Map<String, Object> cfg = new HashMap<String, Object>( TestHelper.getDefaultTestSettings() );
		cfg.put( OgmProperties.DATABASE, "test" );
		cfg.put( OgmProperties.HOST, "www.hibernate.org" );
		cfg.put( OgmProperties.USERNAME, "notauser" );
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, "alhdfoiehfnl" );

		error.expect( ServiceException.class );
		error.expectMessage( "OGM000072" );
		//nested exception
		error.expectCause( hasMessage( containsString( "OGM000051" ) ) );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testConnectionErrorWrappedInHibernateException() throws Exception {
		Map<String, Object> cfg = new HashMap<String, Object>( TestHelper.getDefaultTestSettings() );
		cfg.put( OgmProperties.HOST, NON_EXISTENT_IP );

		error.expect( ServiceException.class );
		error.expectMessage( "OGM000071" );
		//nested exception
		error.expectCause( hasMessage( containsString( "OGM001214" ) ) );

		// will start the service
		TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testConnectionTimeout() {
		Map<String, Object> cfg = new HashMap<String, Object>( TestHelper.getDefaultTestSettings() );
		cfg.put( MongoDBProperties.TIMEOUT, "30" );
		cfg.put( OgmProperties.HOST, NON_EXISTENT_IP );
		cfg.put( OgmProperties.DATABASE, "ogm_test_database" );

		/*
		 * To be sure, the test passes on slow / busy machines the hole
		 * operation should not take more than 3 seconds.
		  */
		final long estimateSpentTime = 3L * 1000L * 1000L * 1000L;

		Exception exception = null;
		final long start = System.nanoTime();
		try {
			// will start the service
			TestHelper.getDefaultTestStandardServiceRegistry( cfg ).getService( DatastoreProvider.class );
		}
		catch ( Exception e ) {
			exception = e;
			assertThat( System.nanoTime() - start ).isLessThanOrEqualTo( estimateSpentTime );
		}
		if ( exception == null ) {
			fail( "The expected exception has not been raised, a MongoDB instance runs on " + NON_EXISTENT_IP );
		}
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
			catch (Exception e) {
			}
		}
	}
}
