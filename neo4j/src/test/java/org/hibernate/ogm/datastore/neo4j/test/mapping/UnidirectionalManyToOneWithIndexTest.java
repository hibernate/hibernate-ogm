/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.associations.collection.types.Child;
import org.hibernate.ogm.backendtck.associations.collection.types.Father;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class UnidirectionalManyToOneWithIndexTest extends Neo4jJpaTestCase {

	private Father father1;
	private Child child11;
	private Child child12;

	private Father father2;
	private Child child21;
	private Child child22;

	@Before
	public void prepareDb() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		child11 = new Child();
		child11.setName( "Emmanuel" );
		em.persist( child11 );

		child12 = new Child();
		child12.setName( "Christophe" );
		em.persist( child12 );

		father1 = new Father();
		father1.getOrderedChildren().add( child11 );
		father1.getOrderedChildren().add( child12 );

		em.persist( father1 );

		child21 = new Child();
		child21.setName( "Caroline" );
		em.persist( child21 );

		child22 = new Child();
		child22.setName( "Thomas" );
		em.persist( child22 );

		father2 = new Father();
		father2.getOrderedChildren().add( child21 );
		father2.getOrderedChildren().add( child22 );

		em.persist( father2 );
		commitOrRollback( true );

		assertNumberOfNodes( 6 );
		assertRelationships( 4 );
	}

	@Test
	public void testMapping() throws Exception {
		String father1Node = "(:Father:ENTITY {id: '" + father1.getId() + "' })";
		String child1Node = "(:Child:ENTITY {id: '" + child11.getId() + "', name: '" + child11.getName() + "' })";
		String child2Node = "(:Child:ENTITY {id: '" + child12.getId() + "', name: '" + child12.getName() + "' })";

		assertExpectedMapping( father1Node + " - [:Father_child {birthorder: 0}] - " + child1Node );
		assertExpectedMapping( father1Node + " - [:Father_child {birthorder: 1}] - " + child2Node );

		String father2Node = "(:Father:ENTITY {id: '" + father2.getId() + "' })";
		String child3Node = "(:Child:ENTITY {id: '" + child21.getId() + "', name: '" + child21.getName() + "' })";
		String child4Node = "(:Child:ENTITY {id: '" + child22.getId() + "', name: '" + child22.getName() + "' })";

		assertExpectedMapping( father2Node + " - [:Father_child {birthorder: 0}] - " + child3Node );
		assertExpectedMapping( father2Node + " - [:Father_child {birthorder: 1}] - " + child4Node );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { Father.class, Child.class };
	}

}
