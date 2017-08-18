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

import javax.persistence.EntityManager;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelper;
import org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelperDelegate;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;

/**
 * Common methods to check the mapping of entities in Neo4j.
 *
 * @author Davide D'Alto
 */
public abstract class Neo4jJpaTestCase extends OgmJpaTestCase {

	private static final String NUMBER_OF_NODES_QUERY = "MATCH (n) RETURN COUNT(*) as count";

	private static final String NUMBER_OF_RELATIONSHIPS_QUERY = "MATCH (n) - [r] -> () RETURN COUNT(r) as count";

	private Neo4jTestHelperDelegate delegate;

	@Before
	public void initDelegate() {
		delegate = Neo4jTestHelper.delegate();
	}

	@After
	public void deleteAll() throws Exception {
		DatastoreProvider datastoreProvider = datastoreProvider();
		delegate.deleteAllElements( datastoreProvider );
	}

	protected void assertNumberOfRelationships(int rel) throws Exception {
		assertThat( numberOfRelationships() ).as( "Unexpected number of relationships" ).isEqualTo( rel );
	}

	protected void assertNumberOfNodes(int nodes) throws Exception {
		assertThat( numberOfNodes() ).as( "Unexpected number of nodes" ).isEqualTo( nodes );
	}

	protected Long numberOfNodes() throws Exception {
		DatastoreProvider datastoreProvider = datastoreProvider();
		return delegate.executeCountQuery( datastoreProvider, NUMBER_OF_NODES_QUERY );
	}

	protected Long numberOfRelationships() throws Exception {
		DatastoreProvider datastoreProvider = datastoreProvider();
		return delegate.executeCountQuery( datastoreProvider, NUMBER_OF_RELATIONSHIPS_QUERY );
	}

	protected void executeCypherQuery(String query, Map<String, Object> parameters) throws Exception {
		DatastoreProvider datastoreProvider = datastoreProvider();
		delegate.executeCypherQuery( datastoreProvider, query, parameters );
	}

	private DatastoreProvider datastoreProvider() {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) getFactory();
		if ( sessionFactory != null ) {
			DatastoreProvider datastoreProvider = sessionFactory.getServiceRegistry().getService( DatastoreProvider.class );
			return datastoreProvider;
		}
		return null;
	}

	protected void assertThatOnlyTheseNodesExist(NodeForGraphAssertions... nodes) throws Exception {
		DatastoreProvider datastoreProvider = datastoreProvider();
		for ( NodeForGraphAssertions node : nodes ) {
			assertThatExists( delegate, datastoreProvider, node );
		}
		assertNumberOfNodes( nodes.length );
	}

	protected void assertThatOnlyTheseRelationshipsExist(RelationshipsChainForGraphAssertions... relationships) throws Exception {
		DatastoreProvider datastoreProvider = datastoreProvider();
		int expectedNumberOfRelationships = 0;
		for ( RelationshipsChainForGraphAssertions relationship : relationships ) {
			assertThatExists( delegate, datastoreProvider, relationship );
			expectedNumberOfRelationships += relationship.getSize();
		}
		assertNumberOfRelationships( expectedNumberOfRelationships );
	}

	protected void persist(EntityManager em, Object... entities) {
		for ( Object entity : entities ) {
			em.persist( entity );
		}
	}
}
