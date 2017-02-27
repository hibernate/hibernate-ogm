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

import org.hibernate.ogm.backendtck.associations.collection.manytomany.ClassRoom;
import org.hibernate.ogm.backendtck.associations.collection.manytomany.Student;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class UnidirectionalManyToManyTest extends Neo4jJpaTestCase {

	private Student john;
	private Student kate;
	private Student mario;

	private ClassRoom math;
	private ClassRoom english;

	@Before
	public void prepareDb() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		john = new Student( "john", "John Doe" );
		kate = new Student( "kate", "Kate Doe" );
		mario = new Student( "mario", "Mario Rossi" );

		math = new ClassRoom( 1L, "Math" );
		math.getStudents().add( john );
		math.getStudents().add( mario );

		english = new ClassRoom( 2L, "English" );
		english.getStudents().add( kate );
		english.getStudents().add( mario );

		persist( em, john, kate, mario, english, math );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions johnNode = node( "john", Student.class.getSimpleName(), ENTITY.name() )
				.property( "id", john.getId() )
				.property( "name", john.getName() );

		NodeForGraphAssertions marioNode = node( "mario", Student.class.getSimpleName(), ENTITY.name() )
				.property( "id", mario.getId() )
				.property( "name", mario.getName() );

		NodeForGraphAssertions kateNode = node( "kate", Student.class.getSimpleName(), ENTITY.name() )
				.property( "id", kate.getId() )
				.property( "name", kate.getName() );

		NodeForGraphAssertions mathNode = node( "math", ClassRoom.class.getSimpleName(), ENTITY.name() )
				.property( "id", math.getId() )
				.property( "name", math.getName() );

		NodeForGraphAssertions englishNode = node( "english", ClassRoom.class.getSimpleName(), ENTITY.name() )
				.property( "id", english.getId() )
				.property( "name", english.getName() );

		RelationshipsChainForGraphAssertions englishToMario = englishNode.relationshipTo( marioNode, "students" );
		RelationshipsChainForGraphAssertions englishToKate = englishNode.relationshipTo( kateNode, "students" );

		RelationshipsChainForGraphAssertions mathToMario = mathNode.relationshipTo( marioNode, "students" );
		RelationshipsChainForGraphAssertions mathToJohn = mathNode.relationshipTo( johnNode, "students" );

		assertThatOnlyTheseNodesExist( johnNode, kateNode, marioNode, mathNode, englishNode );
		assertThatOnlyTheseRelationshipsExist( englishToMario, englishToKate, mathToJohn, mathToMario );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Student.class, ClassRoom.class };
	}

}
