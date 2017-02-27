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

import org.hibernate.ogm.backendtck.associations.manytoone.JUG;
import org.hibernate.ogm.backendtck.associations.manytoone.Member;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class UnidirectionalManyToOneTest extends Neo4jJpaTestCase {

	private JUG jug;
	private Member emmanuel;
	private Member jerome;

	@Before
	public void prepareDb() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		jug = new JUG( "summer_camp" );
		jug.setName( "JUG Summer Camp" );
		em.persist( jug );

		emmanuel = new Member( "emmanuel" );
		emmanuel.setName( "Emmanuel Bernard" );
		emmanuel.setMemberOf( jug );

		jerome = new Member( "jerome" );
		jerome.setName( "Jerome" );
		jerome.setMemberOf( jug );

		em.persist( emmanuel );
		em.persist( jerome );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions jugNode = node( "jug", JUG.class.getSimpleName(), ENTITY.name() )
				.property( "jug_id", jug.getId() )
				.property( "name", jug.getName() );

		NodeForGraphAssertions emmanuelNode = node( "emmanuel", Member.class.getSimpleName(), ENTITY.name() )
				.property( "member_id", emmanuel.getId() )
				.property( "name", emmanuel.getName() );

		NodeForGraphAssertions jeromeNode = node( "jerome", Member.class.getSimpleName(), ENTITY.name() )
				.property( "member_id", jerome.getId() )
				.property( "name", jerome.getName() );

		RelationshipsChainForGraphAssertions relationship1 = emmanuelNode.relationshipTo( jugNode, "memberOf" );
		RelationshipsChainForGraphAssertions relationship2 = jeromeNode.relationshipTo( jugNode, "memberOf" );

		assertThatOnlyTheseNodesExist( jugNode, emmanuelNode, jeromeNode );
		assertThatOnlyTheseRelationshipsExist( relationship1, relationship2 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { JUG.class, Member.class };
	}

}
