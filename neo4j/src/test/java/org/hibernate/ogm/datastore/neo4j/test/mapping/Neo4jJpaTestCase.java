/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.assertThatExists;

import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelper;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.jpa.impl.OgmEntityManagerFactory;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.After;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

/**
 * Common methods to check the mapping of entities in Neo4j.
 *
 * @author Davide D'Alto
 */
public abstract class Neo4jJpaTestCase extends JpaTestCase {

	private static final Log log = LoggerFactory.getLogger();

	@Override
	protected void refineInfo(GetterPersistenceUnitInfo info) {
		info.getProperties().setProperty( Neo4jProperties.DATABASE_PATH, Neo4jTestHelper.dbLocation() );
	}

	@After
	public void deleteAll() throws Exception {
		DatastoreProvider datastoreProvider = datastoreProvider();
		Neo4jTestHelper.deleteAllElements( datastoreProvider );
	}

	protected void assertNumberOfRelationships(int rel) throws Exception {
		assertThat( numberOfRelationships() ).as( "Unexpected number of relationships" ).isEqualTo( rel );
	}

	protected void assertNumberOfNodes(int nodes) throws Exception {
		assertThat( numberOfNodes() ).as( "Unexpected number of nodes" ).isEqualTo( nodes );
	}

	protected Long numberOfNodes() throws Exception {
		return executeCount( "MATCH (n) RETURN COUNT(*) as count" );
	}

	protected Long numberOfRelationships() throws Exception {
		return executeCount( "MATCH (n) - [r] -> () RETURN COUNT(r) as count" );
	}

	protected void executeCypherQuery(String query, Map<String, Object> parameters) throws Exception {
		DatastoreProvider datastoreProvider = datastoreProvider();
		if ( datastoreProvider instanceof Neo4jDatastoreProvider ) {
			Neo4jDatastoreProvider provider = (Neo4jDatastoreProvider) datastoreProvider;
			GraphDatabaseService engine = provider.getDatabase();
			try {
				engine.execute( query, parameters );
			}
			catch (QueryExecutionException qe) {
				throw log.nativeQueryException( qe.getStatusCode(), qe.getMessage(), qe );
			}
		}
	}

	protected GraphDatabaseService createExecutionEngine() {
		DatastoreProvider datastoreProvider = datastoreProvider();
		Neo4jDatastoreProvider provider = (Neo4jDatastoreProvider) datastoreProvider;
		return provider.getDatabase();
	}

	private DatastoreProvider datastoreProvider() {
		OgmEntityManagerFactory emFactory = ( (OgmEntityManagerFactory) getFactory() );
		if ( emFactory != null ) {
			SessionFactoryImplementor sessionFactory = emFactory.getSessionFactory();
			DatastoreProvider datastoreProvider = sessionFactory.getServiceRegistry().getService( DatastoreProvider.class );
			return datastoreProvider;
		}
		return null;
	}

	private Long executeCount(String queryString) throws Exception {
		GraphDatabaseService graphDb = createExecutionEngine();
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

	protected void assertThatOnlyTheseNodesExist(NodeForGraphAssertions... nodes) throws Exception {
		for ( NodeForGraphAssertions node : nodes ) {
			assertThatExists( createExecutionEngine(), node );
		}
		assertNumberOfNodes( nodes.length );
	}

	protected void assertThatOnlyTheseRelationshipsExist(RelationshipsChainForGraphAssertions... relationships) throws Exception {
		int expectedNumberOfRelationships = 0;
		for ( RelationshipsChainForGraphAssertions relationship : relationships ) {
			assertThatExists( createExecutionEngine(), relationship );
			expectedNumberOfRelationships += relationship.getSize();
		}
		assertNumberOfRelationships( expectedNumberOfRelationships );
	}
}
