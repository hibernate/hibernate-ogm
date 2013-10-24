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
package org.hibernate.ogm.test;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.fest.util.Files;
import org.hibernate.ogm.datastore.neo4j.Environment;
import org.hibernate.ogm.datastore.neo4j.impl.EmbeddedGraphDatabaseFactory;
import org.hibernate.ogm.test.utils.Neo4jTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test that it is possible to create and initialize the {@link EmbeddedGraphDatabaseFactory} without exceptions.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class EmbeddedGraphDatabaseFactoryTest {

	String dbLocation = null;

	@Before
	public void setup() {
		dbLocation = Neo4jTestHelper.dbLocation();
	}

	@After
	public void tearDown() {
		Files.delete( new File( dbLocation ) );
	}

	@Test
	public void testLoadPropertiesFromUrl() throws Exception {
		EmbeddedGraphDatabaseFactory factory = new EmbeddedGraphDatabaseFactory();
		Properties properties = new Properties();
		properties.put( Environment.NEO4J_DATABASE_PATH, dbLocation );
		properties.put( Environment.NEO4J_CONFIGURATION_LOCATION, neo4jPropertiesUrl().toExternalForm() );
		factory.initialize( properties );
		factory.create().shutdown();
	}

	@Test
	public void testLoadPropertiesFromFilePath() throws Exception {
		EmbeddedGraphDatabaseFactory factory = new EmbeddedGraphDatabaseFactory();
		Properties properties = new Properties();
		properties.put( Environment.NEO4J_DATABASE_PATH, dbLocation );
		properties.put( Environment.NEO4J_CONFIGURATION_LOCATION, neo4jPropertiesUrl().getFile() );
		factory.initialize( properties );
		factory.create().shutdown();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testLoadMalformedPropertiesLocation() throws Exception {
		EmbeddedGraphDatabaseFactory factory = new EmbeddedGraphDatabaseFactory();
		Properties properties = new Properties();
		properties.put( Environment.NEO4J_DATABASE_PATH, dbLocation );
		properties.put( Environment.NEO4J_CONFIGURATION_LOCATION, "aKDJSAGFKJAFLASFlaLfsfaf" );
		factory.initialize( properties );
		factory.create().shutdown();
	}

	private URL neo4jPropertiesUrl() {
		return Thread.currentThread().getContextClassLoader().getClass().getResource( "/neo4j-embedded-test.properties" );
	}

}
