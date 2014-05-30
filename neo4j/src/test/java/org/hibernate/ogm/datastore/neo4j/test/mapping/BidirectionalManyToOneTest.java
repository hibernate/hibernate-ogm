/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.associations.manytoone.SalesForce;
import org.hibernate.ogm.backendtck.associations.manytoone.SalesGuy;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class BidirectionalManyToOneTest extends Neo4jJpaTestCase {

	private SalesGuy simon;
	private SalesGuy eric;
	private SalesForce salesForce;

	@Before
	public void prepareDb() throws Exception {
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();

		salesForce = new SalesForce();
		salesForce.setCorporation( "Red Hat" );
		em.persist( salesForce );

		eric = new SalesGuy();
		eric.setName( "Eric" );
		eric.setSalesForce( salesForce );
		salesForce.getSalesGuys().add( eric );
		em.persist( eric );

		simon = new SalesGuy();
		simon.setName( "Simon" );
		simon.setSalesForce( salesForce );
		salesForce.getSalesGuys().add( simon );
		em.persist( simon );

		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 3 );
		assertRelationships( 2 );

		String forceNode = "(:SalesForce:ENTITY {"
				+ "  id: '" + salesForce.getId() + "'"
				+ ", corporation: '" + salesForce.getCorporation() + "'"
				+ " })";

		String ericNode = "(:SalesGuy:ENTITY {"
				+ "  id: '" + eric.getId() + "'"
				+ ", name: '" + eric.getName() + "'"
				+ ", salesForce_id: '" + salesForce.getId() + "'"
				+ " })";

		String simonNode = "(:SalesGuy:ENTITY {"
				+ "  id: '" + simon.getId() + "'"
				+ ", name: '" + simon.getName() + "'"
				+ ", salesForce_id: '" + salesForce.getId() + "'"
				+ " })";

		assertExpectedMapping( forceNode + " - [:SalesGuy] - " + ericNode );
		assertExpectedMapping( forceNode + " - [:SalesGuy] - " + simonNode );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { SalesForce.class, SalesGuy.class };
	}

}
