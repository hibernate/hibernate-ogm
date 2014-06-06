/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.associations.manytoone.JUG;
import org.hibernate.ogm.backendtck.associations.manytoone.Member;
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
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		jug = new JUG();
		jug.setName( "JUG Summer Camp" );
		em.persist( jug );

		emmanuel = new Member();
		emmanuel.setName( "Emmanuel Bernard" );
		emmanuel.setMemberOf( jug );

		jerome = new Member();
		jerome.setName( "Jerome" );
		jerome.setMemberOf( jug );

		em.persist( emmanuel );
		em.persist( jerome );
		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 3 );
		assertRelationships( 0 );

		assertExpectedMapping( "(n:JUG:ENTITY {jug_id: '" + jug.getId() + "', name: '" + jug.getName() + "' })" );
		assertExpectedMapping( "(n:Member:ENTITY {member_id: '" + emmanuel.getId() + "', name: '" + emmanuel.getName() + "', memberOf_jug_id: '" + jug.getId() + "' })" );
		assertExpectedMapping( "(n:Member:ENTITY {member_id: '" + jerome.getId() + "' , name: '" + jerome.getName() + "', memberOf_jug_id: '" + jug.getId() + "' })" );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { JUG.class, Member.class };
	}

}
