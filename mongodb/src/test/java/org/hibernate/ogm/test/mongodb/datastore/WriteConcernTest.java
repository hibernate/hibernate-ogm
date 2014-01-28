/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.impl.configuration.MongoDBConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.WriteConcern;

/**
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class WriteConcernTest {

	private static Map<String, String> cfg;

	/**
	 * Set the basic configuration properties required to initialize a {@link MongoDBConfiguration}
	 */
	@BeforeClass
	public static void initCfg() {
		cfg = new HashMap<String, String>();
		cfg.put( OgmProperties.HOST, "localhost" );
		cfg.put( OgmProperties.PORT, "27017" );
		cfg.put( OgmProperties.DATABASE, "database" );
	}

	/**
	 * Test the case when the WriteConcern value has not been set at all.
	 * The default value should then be used
	 */
	@Test
	public void testNoConfiguration() {
		cfg.put( MongoDBProperties.WRITE_CONCERN, null );

		MongoDBConfiguration config = new MongoDBConfiguration( cfg );
		assertEquals( config.buildOptions().getWriteConcern(), MongoDBConfiguration.DEFAULT_WRITE_CONCERN );
	}

	/**
	 * Test the case when an invalid value has been set.
	 * The default value should be used.
	 */
	@Test
	public void testWrongConfiguration() {
		cfg.put( MongoDBProperties.WRITE_CONCERN, "wrongValue" );

		MongoDBConfiguration config = new MongoDBConfiguration( cfg );
		assertEquals( config.buildOptions().getWriteConcern(), MongoDBConfiguration.DEFAULT_WRITE_CONCERN );
	}

	/**
	 * Test the case when a correct value has been set
	 * It should translate the string property into the right WriteConcern value
	 */
	@Test
	public void testCorrectValue() {
		cfg.put( MongoDBProperties.WRITE_CONCERN, "JOURNAL_SAFE" );

		MongoDBConfiguration config = new MongoDBConfiguration( cfg );
		assertEquals( config.buildOptions().getWriteConcern(), WriteConcern.JOURNAL_SAFE );
	}
}
