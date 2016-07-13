/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.associations.manytoone.SalesForce;
import org.hibernate.ogm.backendtck.associations.manytoone.SalesGuy;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
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
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		salesForce = new SalesForce( "red_hat" );
		salesForce.setCorporation( "Red Hat" );
		em.persist( salesForce );

		eric = new SalesGuy( "eric" );
		eric.setName( "Eric" );
		eric.setSalesForce( salesForce );
		salesForce.getSalesGuys().add( eric );
		em.persist( eric );

		simon = new SalesGuy( "simon" );
		simon.setName( "Simon" );
		simon.setSalesForce( salesForce );
		salesForce.getSalesGuys().add( simon );
		em.persist( simon );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions forceNode = node( "force", SalesForce.class.getSimpleName(), ENTITY.name() )
				.property( "id", salesForce.getId() )
				.property( "corporation", salesForce.getCorporation() );

		NodeForGraphAssertions ericNode = node( "eric", SalesGuy.class.getSimpleName(), ENTITY.name() )
				.property( "id", eric.getId() )
				.property( "name", eric.getName() );

		NodeForGraphAssertions simonNode = node( "simon", SalesGuy.class.getSimpleName(), ENTITY.name() )
				.property( "id", simon.getId() )
				.property( "name", simon.getName() );

		RelationshipsChainForGraphAssertions relationship1 = ericNode.relationshipTo( forceNode, "salesForce" );
		RelationshipsChainForGraphAssertions relationship2 = simonNode.relationshipTo( forceNode, "salesForce" );

		assertThatOnlyTheseNodesExist( forceNode, ericNode, simonNode );
		assertThatOnlyTheseRelationshipsExist( relationship1, relationship2 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { SalesForce.class, SalesGuy.class };
	}

}
