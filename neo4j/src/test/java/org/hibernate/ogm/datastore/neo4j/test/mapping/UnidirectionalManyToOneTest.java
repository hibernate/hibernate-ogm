/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import java.util.HashMap;
import java.util.Map;

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
		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 3 );
		assertRelationships( 2 );

		String jugNode = "(jug:JUG:ENTITY {jug_id: {jug}.jug_id, name: {jug}.name })";
		String emmanuelNode = "(e:Member:ENTITY {member_id: {e}.member_id, name: {e}.name})";
		String jeromeNode = "(j:Member:ENTITY {member_id: {j}.member_id, name: {j}.name})";

		Map<String, Object> jugProperties = new HashMap<String, Object>();
		jugProperties.put( "jug_id", jug.getId() );
		jugProperties.put( "name", jug.getName() );

		Map<String, Object> emmanuelProperties = new HashMap<String, Object>();
		emmanuelProperties.put( "member_id", emmanuel.getId() );
		emmanuelProperties.put( "name", emmanuel.getName() );

		Map<String, Object> jeromeProperties = new HashMap<String, Object>();
		jeromeProperties.put( "member_id", jerome.getId() );
		jeromeProperties.put( "name", jerome.getName() );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "jug", jugProperties );
		params.put( "e", emmanuelProperties );
		params.put( "j", jeromeProperties );

		assertExpectedMapping( "jug", jugNode, params );
		assertExpectedMapping( "e", emmanuelNode, params );
		assertExpectedMapping( "j", jeromeNode, params );
		assertExpectedMapping( "r", jugNode + " - [r:memberOf] - " + emmanuelNode, params );
		assertExpectedMapping( "r", jugNode + " - [r:memberOf] - " + jeromeNode, params );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { JUG.class, Member.class };
	}

}
