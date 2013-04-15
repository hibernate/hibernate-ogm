/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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

import java.util.Map;

import com.mongodb.WriteConcern;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.mongodb.Environment;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.test.utils.TestHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

/**
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class WriteConcernTest {

	@Rule
	public ExpectedException error = ExpectedException.none();

	private void runConfigurationTest(String property, WriteConcern expectedValue, String expectErrorCode) {
		Map<String, String> cfg = TestHelper.getEnvironmentProperties();
		cfg.put( Environment.MONGODB_HOST, "203.0.113.1" );
		cfg.put( Environment.MONGODB_PORT, "27017");
		cfg.put( Environment.MONGODB_DATABASE, "ogm_test_database" );
		if ( property != null ) {
			cfg.put( Environment.MONGODB_WRITE_CONCERN, property );
		}
		MongoDBDatastoreProvider provider = new MongoDBDatastoreProvider();
		provider.configure( cfg );

		// Because the host should not exist, we expect to get this exception
		error.expect( HibernateException.class );
		error.expectMessage( expectErrorCode );

		provider.start();
		try {
		WriteConcern writeConcern = provider.mongo.getMongoOptions().getWriteConcern();
		assertEquals( writeConcern, expectedValue );
		} catch (HibernateException e) {
			assertThat( e.getMessage(), containsString( "OGM001216" ) );
		}
	}

	@Test
	public void testNoConfiguration() {
		this.runConfigurationTest( null, WriteConcern.ACKNOWLEDGED, "OGM001214" );
	}

	@Test
	public void testWrongConfiguration() {
		this.runConfigurationTest( "wrongValue", null, "OGM001203" );
	}

	@Test
	public void testCorrectValue() {
		this.runConfigurationTest( "JOURNAL_SAFE", WriteConcern.JOURNAL_SAFE, "OGM001214" );
	}


}
