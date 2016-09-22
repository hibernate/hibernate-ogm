/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.EMBEDDED;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.associations.collection.types.Child;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandChild;
import org.hibernate.ogm.backendtck.associations.collection.types.GrandMother;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class ElementCollectionListWithIndexTest extends Neo4jJpaTestCase {

	private GrandMother granny;
	private GrandChild luke;
	private GrandChild leia;

	@Before
	public void prepareDb() throws Exception {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		luke = new GrandChild();
		luke.setName( "Luke" );

		leia = new GrandChild();
		leia.setName( "Leia" );

		granny = new GrandMother();
		granny.getGrandChildren().add( luke );
		granny.getGrandChildren().add( leia );

		em.persist( granny );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions grannyNode = node( "granny", GrandMother.class.getSimpleName(), ENTITY.name() )
				.property( "id", granny.getId() );

		NodeForGraphAssertions child0Node = node( "child0", "GrandMother_grandChildren", EMBEDDED.name() )
				.property( "name", granny.getGrandChildren().get( 0 ).getName() );

		NodeForGraphAssertions child1Node = node( "child1", "GrandMother_grandChildren", EMBEDDED.name() )
				.property( "name", granny.getGrandChildren().get( 1 ).getName() );

		RelationshipsChainForGraphAssertions relationship1 = grannyNode.relationshipTo( child0Node, "grandChildren" ).property( "birthorder", 0 );
		RelationshipsChainForGraphAssertions relationship2 = grannyNode.relationshipTo( child1Node, "grandChildren" ).property( "birthorder", 1 );

		assertThatOnlyTheseNodesExist( grannyNode, child0Node, child1Node );
		assertThatOnlyTheseRelationshipsExist( relationship1, relationship2 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { GrandMother.class, Child.class };
	}
}
