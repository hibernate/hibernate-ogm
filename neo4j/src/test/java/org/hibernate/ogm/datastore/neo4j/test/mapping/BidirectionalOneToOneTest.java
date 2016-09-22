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

import org.hibernate.ogm.backendtck.associations.onetoone.Husband;
import org.hibernate.ogm.backendtck.associations.onetoone.Wife;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class BidirectionalOneToOneTest extends Neo4jJpaTestCase {

	private Husband husband;
	private Wife wife;

	@Before
	public void prepareDb() throws Exception {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		husband = new Husband( "frederic" );
		husband.setName( "Frederic Joliot-Curie" );

		wife = new Wife( "wife" );
		wife.setName( "Irene Joliot-Curie" );
		wife.setHusband( husband );
		husband.setWife( wife );
		em.persist( husband );
		em.persist( wife );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions wifeNode = node( "wife", Wife.class.getSimpleName(), ENTITY.name() )
				.property( "id", wife.getId() )
				.property( "name", wife.getName() );

		NodeForGraphAssertions husbandNode = node( "husband", Husband.class.getSimpleName(), ENTITY.name() )
				.property( "id", husband.getId() )
				.property( "name", husband.getName() );

		RelationshipsChainForGraphAssertions relationship = husbandNode.relationshipTo( wifeNode, "wife" );

		assertThatOnlyTheseNodesExist( husbandNode, wifeNode );
		assertThatOnlyTheseRelationshipsExist( relationship );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Husband.class, Wife.class };
	}

}
