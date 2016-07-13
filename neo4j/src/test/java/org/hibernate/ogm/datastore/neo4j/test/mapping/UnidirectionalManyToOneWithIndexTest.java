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

import org.hibernate.ogm.backendtck.associations.collection.types.Child;
import org.hibernate.ogm.backendtck.associations.collection.types.Father;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
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
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

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
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions father1Node = node( "father1", Father.class.getSimpleName(), ENTITY.name() )
				.property( "id", father1.getId() );

		NodeForGraphAssertions child11Node = node( "child11", Child.class.getSimpleName(), ENTITY.name() )
				.property( "id", child11.getId() )
				.property( "name", child11.getName() );

		NodeForGraphAssertions child12Node = node( "child12", Child.class.getSimpleName(), ENTITY.name() )
				.property( "id", child12.getId() )
				.property( "name", child12.getName() );

		NodeForGraphAssertions father2Node = node( "father2", Father.class.getSimpleName(), ENTITY.name() )
				.property( "id", father2.getId() );

		NodeForGraphAssertions child21Node = node( "child21", Child.class.getSimpleName(), ENTITY.name() )
				.property( "id", child21.getId() )
				.property( "name", child21.getName() );

		NodeForGraphAssertions child22Node = node( "child22", Child.class.getSimpleName(), ENTITY.name() )
				.property( "id", child22.getId() )
				.property( "name", child22.getName() );

		RelationshipsChainForGraphAssertions relationship1 = father1Node.relationshipTo( child11Node, "orderedChildren" ).property( "birthorder", 0 );
		RelationshipsChainForGraphAssertions relationship2 = father1Node.relationshipTo( child12Node, "orderedChildren" ).property( "birthorder", 1 );
		RelationshipsChainForGraphAssertions relationship3 = father2Node.relationshipTo( child21Node, "orderedChildren" ).property( "birthorder", 0 );
		RelationshipsChainForGraphAssertions relationship4 = father2Node.relationshipTo( child22Node, "orderedChildren" ).property( "birthorder", 1 );

		assertThatOnlyTheseNodesExist( father1Node, child11Node, child12Node, father2Node, child21Node, child22Node );
		assertThatOnlyTheseRelationshipsExist( relationship1, relationship2, relationship3, relationship4 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Father.class, Child.class };
	}

}
