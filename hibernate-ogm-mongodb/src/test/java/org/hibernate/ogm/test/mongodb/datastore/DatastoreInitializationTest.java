/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.mongodb.datastore;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.mongodb.Environment;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.test.utils.TestHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DatastoreInitializationTest {

	@Rule
	public ExpectedException error = ExpectedException.none();

	@Test
	public void testAuthentication() throws Exception {
		Map<String, String> cfg = TestHelper.getEnvironmentProperties();
		cfg.put( Environment.MONGODB_DATABASE, "test" );
		cfg.put( Environment.MONGODB_USERNAME, "notauser" );
		cfg.put( Environment.MONGODB_PASSWORD, "test" );
		MongoDBDatastoreProvider provider = new MongoDBDatastoreProvider();
		provider.configure( cfg );
		error.expect( HibernateException.class );
		error.expectMessage( "OGM001213" );
		provider.start();
	}

	@Test
	public void testConnectionErrorWrappedInHibernateException() throws Exception {
		Properties properties = new Properties();
		properties.load( DatastoreInitializationTest.class.getClassLoader().getResourceAsStream( "hibernate.properties" ) );
		Map<String, String> cfg = TestHelper.getEnvironmentProperties();
		for ( Map.Entry<?,?> entry : properties.entrySet() ) {
			cfg.put( (String) entry.getKey(), (String) entry.getValue() );
		}
		//IP is a test IP that is never assigned
		cfg.put( Environment.MONGODB_HOST, "203.0.113.1" );
		//FIXME put the timeout setting as soon as OGM-219 is implemented
		MongoDBDatastoreProvider provider = new MongoDBDatastoreProvider();
		provider.configure( cfg );
		error.expect( HibernateException.class );
		error.expectMessage( "OGM001214" );
		provider.start();
	}

	@Test
	public void testConnectionTimeout() {
		/**
		 * The timeout used by the driver is set at 30ms.
		 * The IP address 203.0.113.1 has been chosen because of:
		 * "203.0.113.0/24 No Assigned as "TEST-NET-3" in RFC 5737 for use solely in documentation and
		 * example source code and should not be used publicly."
		 */
		String host = "203.0.113.1";

		Map<String, Object> cfg = new HashMap<String, Object>();
		cfg.put( Environment.MONGODB_TIMEOUT, "30" );
		cfg.put( Environment.MONGODB_HOST, host );
		cfg.put( Environment.MONGODB_DATABASE, "ogm_test_database" );
		MongoDBDatastoreProvider provider = new MongoDBDatastoreProvider();

		/*
		 * To be sure, the test passes on slow / busy machines the hole
		 * operation should not take more than 3 seconds.
		  */
		final long estimateSpentTime = 3L * 1000L * 1000L * 1000L;
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
			fail( "The expected exception has not been raised, a MongoDB instance runs on " + host );
		}
	}
}

