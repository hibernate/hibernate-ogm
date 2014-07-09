/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import org.fest.util.Files;
import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.impl.EmbeddedGraphDatabaseFactory;
import org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test that it is possible to create and initialize the {@link EmbeddedGraphDatabaseFactory} without exceptions.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
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
		properties.put( Neo4jProperties.DATABASE_PATH, dbLocation );
		properties.put( Neo4jProperties.CONFIGURATION_RESOURCE_NAME, neo4jPropertiesUrl().toExternalForm() );
		factory.initialize( properties );
		factory.create().shutdown();
	}

	@Test
	public void testLoadPropertiesFromFilePath() throws Exception {
		EmbeddedGraphDatabaseFactory factory = new EmbeddedGraphDatabaseFactory();
		Properties properties = new Properties();
		properties.put( Neo4jProperties.DATABASE_PATH, dbLocation );
		properties.put( Neo4jProperties.CONFIGURATION_RESOURCE_NAME, neo4jPropertiesUrl().getFile() );
		factory.initialize( properties );
		factory.create().shutdown();
	}

	@Test(expected = HibernateException.class)
	public void testLoadMalformedPropertiesLocation() throws Exception {
		EmbeddedGraphDatabaseFactory factory = new EmbeddedGraphDatabaseFactory();
		Properties properties = new Properties();
		properties.put( Neo4jProperties.DATABASE_PATH, dbLocation );
		properties.put( Neo4jProperties.CONFIGURATION_RESOURCE_NAME, "aKDJSAGFKJAFLASFlaLfsfaf" );
		factory.initialize( properties );
		factory.create().shutdown();
	}

	private URL neo4jPropertiesUrl() {
		return Thread.currentThread().getContextClassLoader().getClass().getResource( "/neo4j-embedded-test.properties" );
	}

}
