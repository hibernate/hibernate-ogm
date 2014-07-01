/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.After;

/**
 * Common methods to check the mapping of entities in Neo4j.
 *
 * @author Davide D'Alto
 */
public abstract class Neo4jJpaTestCase extends JpaTestCase {

	@After
	public void after() throws Exception {
		assertNoTempNodeExists();
		deleteAll();
	}

	private void deleteAll() throws Exception {
		executeQuery( "MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n, r" );
	}

	protected void assertRelationships(int rel) throws Exception {
		assertThat( numberOfRelationships() ).as( "Unexpected number of relationships" ).isEqualTo( rel );
	}

	protected void assertNumberOfNodes(int nodes) throws Exception {
		assertThat( numberOfNodes() ).as( "Unexpected number of nodes" ).isEqualTo( nodes );
	}

	protected void assertNoTempNodeExists() throws Exception {
		assertThat( executeQuery( "MATCH (n:" + NodeLabel.TEMP_NODE + ") RETURN 1" ) ).as( "No temp node should exist at the end of the test" ).isNull();
	}

	protected void assertExpectedMapping(String element) throws Exception {
		assertThat( executeQuery( "MATCH " + element + " RETURN 1" ) ).as( "Not found in the db: " + element ).isNotNull();
	}

	protected Long numberOfNodes() throws Exception {
		return executeQuery( "MATCH (n) RETURN COUNT(*)" );
	}

	protected Long numberOfRelationships() throws Exception {
		return executeQuery( "MATCH (n) - [r] -> () RETURN COUNT(r)" );
	}

	private Long executeQuery(String queryString) throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		@SuppressWarnings("unchecked")
		List<Object> results = em.createNativeQuery( queryString ).getResultList();
		Long uniqueResult = null;
		if ( !results.isEmpty() ) {
			uniqueResult = (Long) results.get( 0 );
		}
		commitOrRollback( true );
		em.close();
		if ( uniqueResult == null ) {
			return null;
		}
		return uniqueResult;
	}

}
