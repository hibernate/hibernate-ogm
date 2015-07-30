/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.datastore;

import java.util.Map;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.configuration.impl.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.options.AuthenticationMechanismType;
import org.hibernate.ogm.options.navigation.impl.OptionsServiceImpl;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.utils.SkipByDatastoreProvider;
import org.hibernate.ogm.utils.SkippableTestRunner;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

	private Map<String, String> cfg;

	@Before
	public void setUp() {
		cfg = TestHelper.getEnvironmentProperties();
		cfg.put( OgmProperties.DATABASE, "snafu" );
		cfg.put( OgmProperties.USERNAME, "foo" );
		cfg.put( OgmProperties.PASSWORD, "bar" );
		cfg.put( "hibernate.ogm.mongodb.driver.serverSelectionTimeout", "1000" );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testAuthentication() throws Exception {
		error.expect( ServiceException.class );
		error.expectMessage( "OGM000071" );
		// the timeout exception thrown by the driver will actually contain some information about the authentication
		// error. Obviously quite fragile. Might change
		error.expectCause( hasMessage( containsString( "Exception authenticating" ) ) );

		MongoDBDatastoreProvider provider = new MongoDBDatastoreProvider();
		provider.injectServices( getServiceRegistry( cfg ) );
		provider.configure( cfg );
		provider.start();
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testDefaultAuthenticationMechanism() throws Exception {
		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();
		provider.injectServices( getServiceRegistry( cfg ) );
		provider.configure( cfg );
		provider.start();

		assertThat( provider.leakingClient.getCredentialsList().get( 0 ).getMechanism() ).isEqualTo( null );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testSCRAMSHA1AuthenticationMechanism() throws Exception {
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, AuthenticationMechanismType.SCRAM_SHA_1.name() );

		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();
		provider.injectServices( getServiceRegistry( cfg ) );
		provider.configure( cfg );
		provider.start();

		assertThat(
				provider.leakingClient.getCredentialsList()
						.get( 0 )
						.getMechanism()
		).isEqualTo( MongoCredential.SCRAM_SHA_1_MECHANISM );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testX509AuthenticationMechanism() throws Exception {
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, AuthenticationMechanismType.MONGODB_X509.name() );

		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();
		provider.injectServices( getServiceRegistry( cfg ) );
		provider.configure( cfg );
		provider.start();

		assertThat(
				provider.leakingClient.getCredentialsList()
						.get( 0 )
						.getMechanism()
		).isEqualTo( MongoCredential.MONGODB_X509_MECHANISM );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testGSSAPIAuthenticationMechanism() throws Exception {
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, AuthenticationMechanismType.GSSAPI.name() );

		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();
		provider.injectServices( getServiceRegistry( cfg ) );
		provider.configure( cfg );
		provider.start();

		assertThat(
				provider.leakingClient.getCredentialsList()
						.get( 0 )
						.getMechanism()
		).isEqualTo( MongoCredential.GSSAPI_MECHANISM );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testPlainAuthenticationMechanism() throws Exception {
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, AuthenticationMechanismType.PLAIN.name() );

		LeakingMongoDBDatastoreProvider provider = new LeakingMongoDBDatastoreProvider();
		provider.injectServices( getServiceRegistry( cfg ) );
		provider.configure( cfg );
		provider.start();

		assertThat(
				provider.leakingClient.getCredentialsList()
						.get( 0 )
						.getMechanism()
		).isEqualTo( MongoCredential.PLAIN_MECHANISM );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testNotRecognizedAuthenticationMechanism() throws Exception {
		cfg.put( MongoDBProperties.AUTHENTICATION_MECHANISM, "alhdfoiehfnl" );

		MongoDBDatastoreProvider provider = new MongoDBDatastoreProvider();
		provider.injectServices( getServiceRegistry( cfg ) );

		error.expect( ServiceException.class );
		error.expectMessage( "OGM000072" );
		//nested exception
		error.expectCause( hasMessage( containsString( "OGM000051" ) ) );

		provider.configure( cfg );
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testConnectionErrorWrappedInHibernateException() throws Exception {
		cfg.put( OgmProperties.HOST, NON_EXISTENT_IP );

		MongoDBDatastoreProvider provider = new MongoDBDatastoreProvider();
		provider.injectServices( getServiceRegistry( cfg ) );
		provider.configure( cfg );

		error.expect( ServiceException.class );
		error.expectMessage( "OGM000071" );
		//nested exception
		error.expectCause( hasMessage( containsString( "OGM001214" ) ) );

		provider.start();
	}

	@Test
	@SkipByDatastoreProvider(AvailableDatastoreProvider.FONGO)
	public void testConnectionTimeout() {
		cfg.put( OgmProperties.HOST, NON_EXISTENT_IP );
		cfg.put( OgmProperties.DATABASE, "ogm_test_database" );

		MongoDBDatastoreProvider provider = new MongoDBDatastoreProvider();
		provider.injectServices( getServiceRegistry( cfg ) );
		provider.configure( cfg );

		error.expect( ServiceException.class );
		error.expectMessage( "OGM000071" );
		// the timeout exception thrown by the driver will actually contain some information about the authentication
		// error. Obviously quite fragile. Might change
		error.expectCause( hasMessage( containsString( "Timed out" ) ) );

		provider.start();
	}

	private ServiceRegistryImplementor getServiceRegistry(Map<String, ?> cfg) {
		ServiceRegistryImplementor serviceRegistry = mock( ServiceRegistryImplementor.class );
		when( serviceRegistry.getService( ClassLoaderService.class ) ).thenReturn( new ClassLoaderServiceImpl() );

		OptionsServiceImpl optionsService = new OptionsServiceImpl();
		optionsService.injectServices( serviceRegistry );
		optionsService.configure( cfg );
		when( serviceRegistry.getService( OptionsService.class ) ).thenReturn( optionsService );

		return serviceRegistry;
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
