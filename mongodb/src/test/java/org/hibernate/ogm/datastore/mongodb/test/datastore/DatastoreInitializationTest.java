/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012-2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.mongodb.test.datastore;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.options.navigation.impl.WritableOptionsServiceContext;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.test.utils.TestHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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
		provider.injectServices( getServiceRegistry() );
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
		provider.injectServices( getServiceRegistry() );
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
		provider.injectServices( getServiceRegistry() );
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

	private static ServiceRegistryImplementor getServiceRegistry() {
		ServiceRegistryImplementor serviceRegistry = mock( ServiceRegistryImplementor.class );
		OptionsService optionService = mock( OptionsService.class );
		when( optionService.context() ).thenReturn( new WritableOptionsServiceContext() );
		when( serviceRegistry.getService( OptionsService.class ) ).thenReturn( optionService );
		return serviceRegistry;
	}
}
