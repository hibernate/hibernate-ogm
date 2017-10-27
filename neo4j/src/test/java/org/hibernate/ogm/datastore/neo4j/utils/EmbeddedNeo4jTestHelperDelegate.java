/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.utils;

import java.io.File;
import java.util.Map;

import org.fest.util.Files;
import org.hibernate.Session;
import org.hibernate.ogm.datastore.neo4j.EmbeddedNeo4jDialect;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.embedded.impl.EmbeddedNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.service.spi.Stoppable;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

/**
 * @author Davide D'Alto
 */
public class EmbeddedNeo4jTestHelperDelegate implements Neo4jTestHelperDelegate {

	public static final EmbeddedNeo4jTestHelperDelegate INSTANCE = new EmbeddedNeo4jTestHelperDelegate();

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private static final Map<String, String> hibernateProperties = PropertiesReader.getHibernateProperties();

	private static final String ROOT_FOLDER = hibernateProperties.get( Neo4jProperties.DATABASE_PATH ) + File.separator + "NEO4J";

	private EmbeddedNeo4jTestHelperDelegate() {
	}

	@Override
	public long getNumberOfEntities(Session session, DatastoreProvider provider) {
		GraphDatabaseService graphDb = ( (EmbeddedNeo4jDatastoreProvider) provider ).getDatabase();
		ResourceIterator<Long> result = graphDb.execute( ENTITY_COUNT_QUERY ).columnAs( "count" );
		Long count = result.next();
		result.close();
		return count.longValue();
	}

	@Override
	public long getNumberOfAssociations(Session session, DatastoreProvider provider) {
		GraphDatabaseService graphDb = ( (EmbeddedNeo4jDatastoreProvider) provider ).getDatabase();
		ResourceIterator<Long> result = graphDb.execute( ASSOCIATION_COUNT_QUERY ).columnAs( "count" );
		Long count = result.next();
		result.close();
		return count.longValue();
	}

	@Override
	public GridDialect getDialect(DatastoreProvider datastoreProvider) {
		return new EmbeddedNeo4jDialect( (EmbeddedNeo4jDatastoreProvider) datastoreProvider );
	}

	@Override
	public void dropDatabase(DatastoreProvider datastoreProvider) {
		( (Stoppable) datastoreProvider ).stop();
		final String rootFolder = rootFolder( hibernateProperties );
		Files.delete( new File( rootFolder ) );
	}

	private static String rootFolder(Map<String, String> hibernateProperties) {
		String databasePath = hibernateProperties.get( Neo4jProperties.DATABASE_PATH );
		return databasePath( databasePath );
	}

	private static String databasePath(String databasePath) {
		return databasePath + File.separator + "NEO4J";
	}

	@Override
	public void deleteAllElements(DatastoreProvider datastoreProvider) {
		GraphDatabaseService graphDb = createExecutionEngine( datastoreProvider );
		graphDb.execute( DELETE_ALL ).close();
	}

	/**
	 * Returns a random location where to create a neo4j database
	 */
	public static String dbLocation() {
		return ROOT_FOLDER + File.separator + "neo4j-db-" + System.currentTimeMillis();
	}

	@Override
	public Long executeCountQuery(DatastoreProvider datastoreProvider, String queryString) {
		GraphDatabaseService graphDb = createExecutionEngine( datastoreProvider );
		Transaction tx = graphDb.beginTx();
		Result result = graphDb.execute( queryString );
		ResourceIterator<Long> count = result.columnAs( "count" );
		try {
			tx.success();
			return count.next();
		}
		finally {
			try {
				count.close();
			}
			finally {
				tx.close();
			}
		}
	}

	private GraphDatabaseService createExecutionEngine(DatastoreProvider datastoreProvider) {
		return ( (EmbeddedNeo4jDatastoreProvider) datastoreProvider ).getDatabase();
	}

	@Override
	public void executeCypherQuery(DatastoreProvider datastoreProvider, String query, Map<String, Object> parameters) {
		EmbeddedNeo4jDatastoreProvider provider = (EmbeddedNeo4jDatastoreProvider) datastoreProvider;
		GraphDatabaseService engine = provider.getDatabase();
		try {
			engine.execute( query, parameters );
		}
		catch (QueryExecutionException qe) {
			throw log.nativeQueryException( qe.getStatusCode(), qe.getMessage(), qe );
		}
	}

	@Override
	public PropertyContainer findNode(DatastoreProvider datastoreProvider, NodeForGraphAssertions node) {
		String nodeAsCypher = node.toCypher();
		String query = "MATCH " + nodeAsCypher + " RETURN " + node.getAlias();

		GraphDatabaseService engine = createExecutionEngine( datastoreProvider );
		Transaction tx = engine.beginTx();
		try {
			ResourceIterator<Object> nodes = engine.execute( query, node.getParams() ).columnAs( node.getAlias() );

			PropertyContainer propertyContainer = (PropertyContainer) nodes.next();
			if ( nodes.hasNext() ) {
				throw new NotUniqueException();
			}

			tx.success();
			return propertyContainer;
		}
		finally {
			tx.close();
		}
	}

	@Override
	public Map<String, Object> findProperties(DatastoreProvider datastoreProvider, NodeForGraphAssertions node) {
		String nodeAsCypher = node.toCypher();
		String query = "MATCH " + nodeAsCypher + " RETURN " + node.getAlias();

		GraphDatabaseService engine = createExecutionEngine( datastoreProvider );
		Transaction tx = engine.beginTx();
		try {
			ResourceIterator<Object> nodes = engine.execute( query, node.getParams() ).columnAs( node.getAlias() );

			PropertyContainer propertyContainer = (PropertyContainer) nodes.next();
			if ( nodes.hasNext() ) {
				throw new NotUniqueException();
			}

			Map<String, Object> allProperties = propertyContainer.getAllProperties();
			tx.success();
			return allProperties;
		}
		finally {
			tx.close();
		}
	}

	@Override
	public PropertyContainer findRelationshipStartNode(DatastoreProvider datastoreProvider, RelationshipsChainForGraphAssertions relationship) {
		GraphDatabaseService engine = createExecutionEngine( datastoreProvider );
		Transaction tx = engine.beginTx();
		try {
			String relationshipAsCypher = relationship.toCypher();
			NodeForGraphAssertions node = relationship.getStart();
			String query = "MATCH " + relationshipAsCypher + " RETURN " + node.getAlias();
			ResourceIterator<Object> results = engine.execute( query, relationship.getParams() ).columnAs( node.getAlias() );
			if ( !results.hasNext() ) {
				return null;
			}

			PropertyContainer startNode = (PropertyContainer) results.next();
			if ( results.hasNext() ) {
				throw new NotUniqueException();
			}

			tx.success();
			return startNode;
		}
		finally {
			tx.close();
		}
	}
}
