/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.embedded.impl.EmbeddedNeo4jGraphDatabaseFactory;
import org.hibernate.ogm.datastore.neo4j.utils.EmbeddedNeo4jTestHelperDelegate;

import org.junit.Before;
import org.junit.Test;

import org.neo4j.graphdb.GraphDatabaseService;

/**
 * Test that it is possible to create and initialize the {@link EmbeddedNeo4jGraphDatabaseFactory} without exceptions.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class EmbeddedGraphDatabaseFactoryTest {

	String dbLocation = null;

	@Before
	public void setup() {
		dbLocation = EmbeddedNeo4jTestHelperDelegate.dbLocation();
	}

	@Test
	public void testLoadPropertiesFromUrl() throws Exception {
		EmbeddedNeo4jGraphDatabaseFactory factory = new EmbeddedNeo4jGraphDatabaseFactory();
		Properties properties = new Properties();
		properties.put( Neo4jProperties.DATABASE_PATH, dbLocation );
		properties.put( Neo4jProperties.CONFIGURATION_RESOURCE_NAME, neo4jPropertiesUrl().toExternalForm() );
		factory.initialize( properties );
		factory.create().shutdown();
	}

	@Test
	public void testLoadPropertiesFromFilePath() throws Exception {
		EmbeddedNeo4jGraphDatabaseFactory factory = new EmbeddedNeo4jGraphDatabaseFactory();
		Properties properties = new Properties();
		properties.put( Neo4jProperties.DATABASE_PATH, dbLocation );
		properties.put( Neo4jProperties.CONFIGURATION_RESOURCE_NAME, neo4jPropertiesUrl().getFile() );
		factory.initialize( properties );
		factory.create().shutdown();
	}

	@Test(expected = HibernateException.class)
	public void testLoadMalformedPropertiesLocation() throws Exception {
		EmbeddedNeo4jGraphDatabaseFactory factory = new EmbeddedNeo4jGraphDatabaseFactory();
		Properties properties = new Properties();
		properties.put( Neo4jProperties.DATABASE_PATH, dbLocation );
		properties.put( Neo4jProperties.CONFIGURATION_RESOURCE_NAME, "aKDJSAGFKJAFLASFlaLfsfaf" );
		factory.initialize( properties );
		factory.create().shutdown();
	}

	@Test
	public void testInstantiationTwoFactoriesForOnePath() {
		//create first factory
		EmbeddedNeo4jGraphDatabaseFactory factory1 = new EmbeddedNeo4jGraphDatabaseFactory();
		Properties properties = new Properties();
		properties.put( Neo4jProperties.DATABASE_PATH, dbLocation );
		properties.put( Neo4jProperties.CONFIGURATION_RESOURCE_NAME, neo4jPropertiesUrl().toExternalForm() );
		factory1.initialize( properties );
		GraphDatabaseService graphDatabaseService1 = factory1.create();

		//create second factory
		EmbeddedNeo4jGraphDatabaseFactory factory2 = new EmbeddedNeo4jGraphDatabaseFactory();
		factory2.initialize( properties );
		GraphDatabaseService graphDatabaseService2 = factory2.create();

		assertTrue( "Two instances must be same!", graphDatabaseService1 == graphDatabaseService2 );

		graphDatabaseService1.shutdown();
		graphDatabaseService2.shutdown();
	}

	@Test
	public void testInstantiationTwoFactoriesForTwoPaths() {
		//create first factory
		EmbeddedNeo4jGraphDatabaseFactory factory1 = new EmbeddedNeo4jGraphDatabaseFactory();
		Properties properties1 = new Properties();
		properties1.put( Neo4jProperties.DATABASE_PATH, dbLocation );
		properties1.put( Neo4jProperties.CONFIGURATION_RESOURCE_NAME, neo4jPropertiesUrl().toExternalForm() );
		factory1.initialize( properties1 );
		GraphDatabaseService graphDatabaseService1 = factory1.create();

		//create second factory
		EmbeddedNeo4jGraphDatabaseFactory factory2 = new EmbeddedNeo4jGraphDatabaseFactory();
		Properties properties2 = new Properties();
		properties2.put( Neo4jProperties.DATABASE_PATH, dbLocation + "2" );
		properties2.put( Neo4jProperties.CONFIGURATION_RESOURCE_NAME, neo4jPropertiesUrl().toExternalForm() );
		factory2.initialize( properties2 );
		GraphDatabaseService graphDatabaseService2 = factory2.create();

		assertFalse( "Two instances must not be same!", graphDatabaseService1 == graphDatabaseService2 );

		graphDatabaseService1.shutdown();
		graphDatabaseService2.shutdown();
	}

	private URL neo4jPropertiesUrl() {
		return Thread.currentThread().getContextClassLoader().getClass().getResource( "/neo4j-embedded-test.properties" );
	}

}
