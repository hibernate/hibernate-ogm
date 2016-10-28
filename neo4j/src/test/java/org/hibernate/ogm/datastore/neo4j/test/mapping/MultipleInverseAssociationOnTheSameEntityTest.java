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

import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class MultipleInverseAssociationOnTheSameEntityTest extends Neo4jJpaTestCase {

	private MappedNode root, child1, child2;
	private MappedLink link1, link2;

	@Before
	public void setUpTestData() {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		root = new MappedNode( "root" );
		child1 = new MappedNode( "child1" );
		child2 = new MappedNode( "child2" );

		link1 = new MappedLink( "nl1" );
		link1.setSource( root );
		link1.setTarget( child1 );

		link2 = new MappedLink( "nl2" );
		link2.setSource( root );
		link2.setTarget( child2 );

		root.getChildren().add( link1 );
		root.getChildren().add( link2 );

		em.persist( root );
		em.persist( child1 );
		em.persist( child2 );
		em.persist( link1 );
		em.persist( link2 );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions rootNode = node( "r", MappedNode.class.getSimpleName(), ENTITY.name() )
				.property( "name", root.getName() );

		NodeForGraphAssertions child1Node = node( "c1", MappedNode.class.getSimpleName(), ENTITY.name() )
				.property( "name", child1.getName() );

		NodeForGraphAssertions child2Node = node( "c2", MappedNode.class.getSimpleName(), ENTITY.name() )
				.property( "name", child2.getName() );

		NodeForGraphAssertions link1Node = node( "l1", MappedLink.class.getSimpleName(), ENTITY.name() )
				.property( "id", link1.getId() );

		NodeForGraphAssertions link2Node = node( "l2", MappedLink.class.getSimpleName(), ENTITY.name() )
				.property( "id", link2.getId() );

		RelationshipsChainForGraphAssertions relationship1 = link1Node.relationshipTo( rootNode, "source" );
		RelationshipsChainForGraphAssertions relationship2 = link1Node.relationshipTo( child1Node, "target" );

		RelationshipsChainForGraphAssertions relationship3 = link2Node.relationshipTo( rootNode, "source" );
		RelationshipsChainForGraphAssertions relationship4 = link2Node.relationshipTo( child2Node, "target" );

		assertThatOnlyTheseNodesExist( rootNode, child1Node, child2Node, link1Node, link2Node );
		assertThatOnlyTheseRelationshipsExist( relationship1, relationship2, relationship3, relationship4 );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ MappedNode.class, MappedLink.class };
	}
}
