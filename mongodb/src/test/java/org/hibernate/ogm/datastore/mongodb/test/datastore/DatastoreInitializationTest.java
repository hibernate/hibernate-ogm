/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.datastore;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.options.navigation.impl.OptionsServiceImpl;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class DatastoreInitializationTest {

	/**
	 * The IP address 203.0.113.1 has been chosen because of: "203.0.113.0/24 No Assigned as "TEST-NET-3" in RFC 5737
	 * for use solely in documentation and example source code and should not be used publicly."
	 */
	private static final String NON_EXISTENT_IP = "203.0.113.1";

	@Rule
	public ExpectedException error = ExpectedException.none();

	@Test
	public void testAuthentication() throws Exception {
		Map<String, String> cfg = TestHelper.getEnvironmentProperties();
		cfg.put( OgmProperties.DATABASE, "test" );
		cfg.put( OgmProperties.USERNAME, "notauser" );
		cfg.put( OgmProperties.PASSWORD, "test" );

		MongoDBDatastoreProvider provider = new MongoDBDatastoreProvider();
		provider.injectServices( getServiceRegistry( cfg ) );
		provider.configure( cfg );

		error.expect( HibernateException.class );
		error.expectMessage( "OGM001213" );

		provider.start();
	}

	@Test
	public void testConnectionErrorWrappedInHibernateException() throws Exception {
		Map<String, String> cfg = TestHelper.getEnvironmentProperties();
		cfg.put( OgmProperties.HOST, NON_EXISTENT_IP );

		MongoDBDatastoreProvider provider = new MongoDBDatastoreProvider();
		provider.injectServices( getServiceRegistry( cfg ) );
		provider.configure( cfg );

		error.expect( HibernateException.class );
		error.expectMessage( "OGM001214" );

		provider.start();
	}

	@Test
	public void testConnectionTimeout() {
		Map<String, Object> cfg = new HashMap<String, Object>();
		cfg.put( MongoDBProperties.TIMEOUT, "30" );
		cfg.put( OgmProperties.HOST, NON_EXISTENT_IP );
		cfg.put( OgmProperties.DATABASE, "ogm_test_database" );

		MongoDBDatastoreProvider provider = new MongoDBDatastoreProvider();

		/*
		 * To be sure, the test passes on slow / busy machines the hole
		 * operation should not take more than 3 seconds.
		  */
		final long estimateSpentTime = 3L * 1000L * 1000L * 1000L;
		provider.injectServices( getServiceRegistry( cfg ) );
		provider.configure( cfg );

		Exception exception = null;
		final long start = System.nanoTime();
		try {
			provider.start();
		}
		catch ( Exception e ) {
			exception = e;
			assertThat( System.nanoTime() - start ).isLessThanOrEqualTo( estimateSpentTime );
		}
		if ( exception == null ) {
			fail( "The expected exception has not been raised, a MongoDB instance runs on " + NON_EXISTENT_IP );
		}
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
}
