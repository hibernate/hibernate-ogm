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
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.jpa.impl.OgmEntityManagerFactory;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.After;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

/**
 * Common methods to check the mapping of entities in Neo4j.
 *
 * @author Davide D'Alto
 */
public abstract class Neo4jJpaTestCase extends JpaTestCase {

	@After
	public void deleteAll() throws Exception {
		executeQueryUpdate( "MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n, r" );
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

	protected Result executeCypherQuery(String query, Map<String, Object> parameters) throws Exception {
		GraphDatabaseService engine = createExecutionEngine();
		Result result = engine.execute( query, parameters );
		return result;
	}

	protected GraphDatabaseService createExecutionEngine() {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) ( (OgmEntityManagerFactory) getFactory() ).getSessionFactory();
		Neo4jDatastoreProvider provider = (Neo4jDatastoreProvider) sessionFactory.getServiceRegistry().getService( DatastoreProvider.class );
		return provider.getDatabase();
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

	private void executeQueryUpdate(String queryString) throws Exception {
		GraphDatabaseService graphDb = createExecutionEngine();
		try (Transaction tx = graphDb.beginTx()) {
			graphDb.execute( queryString ).close();
			tx.success();
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
