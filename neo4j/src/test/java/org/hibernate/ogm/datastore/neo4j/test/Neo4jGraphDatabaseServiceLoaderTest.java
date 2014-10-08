/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Properties;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.impl.InternalProperties;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jGraphDatabaseServiceFactoryProvider;
import org.hibernate.ogm.datastore.neo4j.spi.GraphDatabaseServiceFactory;
import org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelper;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.graphdb.traversal.BidirectionalTraversalDescription;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.EmbeddedGraphDatabase;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jGraphDatabaseServiceLoaderTest {

	@Test
	public void testEmbeddedIsTheDefaultGraphDatabaseService() throws Exception {
		Properties properties = new Properties();
		properties.put( Neo4jProperties.DATABASE_PATH, Neo4jTestHelper.dbLocation() );
		Neo4jGraphDatabaseServiceFactoryProvider graphService = new Neo4jGraphDatabaseServiceFactoryProvider();
		GraphDatabaseService db = graphService.load( properties, new ClassLoaderServiceImpl() ).create();
		db.shutdown();
		assertThat( db.getClass() ). isEqualTo( EmbeddedGraphDatabase.class );
	}

	@Test
	public void testSelectedGraphDatabaseServiceIsLoaded() throws Exception {
		Properties properties = new Properties();
		properties.put( Neo4jProperties.DATABASE_PATH, Neo4jTestHelper.dbLocation() );
		properties.put( InternalProperties.NEO4J_GRAPHDB_FACTORYCLASS, MockGraphServiceFactory.class.getName() );
		Neo4jGraphDatabaseServiceFactoryProvider graphService = new Neo4jGraphDatabaseServiceFactoryProvider();
		GraphDatabaseService db = graphService.load( properties, new ClassLoaderServiceImpl() ).create();
		db.shutdown();
		assertThat( db.getClass() ). isEqualTo( MockGraphDatabaseService.class );
	}

	@Test
	public void testPropertiesArePassed() throws Exception {
		Properties properties = new Properties();
		properties.put( Neo4jProperties.DATABASE_PATH, Neo4jTestHelper.dbLocation() );
		properties.put( InternalProperties.NEO4J_GRAPHDB_FACTORYCLASS, MockGraphServiceFactory.class.getName() );
		Neo4jGraphDatabaseServiceFactoryProvider graphService = new Neo4jGraphDatabaseServiceFactoryProvider();
		MockGraphDatabaseService db = (MockGraphDatabaseService) graphService.load( properties, new ClassLoaderServiceImpl() ).create();
		db.shutdown();
		assertTrue( "GraphDatabaseService factory cannot read the configuration properties", db.isConfigurationReadable() );
	}

	public static class MockGraphServiceFactory implements GraphDatabaseServiceFactory {

		boolean configurationReadable = false;

		@Override
		public void initialize(Map<?, ?> properties) {
			configurationReadable = MockGraphServiceFactory.class.getName().equals(
					properties.get( InternalProperties.NEO4J_GRAPHDB_FACTORYCLASS ) );
		}

		@Override
		public GraphDatabaseService create() {
			return new MockGraphDatabaseService( configurationReadable );
		}

	}

	public static class MockGraphDatabaseService implements GraphDatabaseService {

		private final boolean configurationReadable;

		public MockGraphDatabaseService(boolean readableConfiguration) {
			this.configurationReadable = readableConfiguration;
		}

		public boolean isConfigurationReadable() {
			return configurationReadable;
		}

		@Override
		public Node createNode() {
			return null;
		}

		@Override
		public Node getNodeById(long id) {
			return null;
		}

		@Override
		public Relationship getRelationshipById(long id) {
			return null;
		}

		@Override
		public Iterable<Node> getAllNodes() {
			return null;
		}

		@Override
		public Iterable<RelationshipType> getRelationshipTypes() {
			return null;
		}

		@Override
		public void shutdown() {
		}

		@Override
		public Transaction beginTx() {
			return null;
		}

		@Override
		public <T> TransactionEventHandler<T> registerTransactionEventHandler(TransactionEventHandler<T> handler) {
			return null;
		}

		@Override
		public <T> TransactionEventHandler<T> unregisterTransactionEventHandler(TransactionEventHandler<T> handler) {
			return null;
		}

		@Override
		public KernelEventHandler registerKernelEventHandler(KernelEventHandler handler) {
			return null;
		}

		@Override
		public KernelEventHandler unregisterKernelEventHandler(KernelEventHandler handler) {
			return null;
		}

		@Override
		public IndexManager index() {
			return null;
		}

		@Override
		public Node createNode(Label... labels) {
			return null;
		}

		@Override
		public ResourceIterable<Node> findNodesByLabelAndProperty(Label label, String key, Object value) {
			return null;
		}

		@Override
		public Schema schema() {
			return null;
		}

		@Override
		public boolean isAvailable(long timeout) {
			return false;
		}

		@Override
		public TraversalDescription traversalDescription() {
			return null;
		}

		@Override
		public BidirectionalTraversalDescription bidirectionalTraversalDescription() {
			return null;
		}

	}
}
