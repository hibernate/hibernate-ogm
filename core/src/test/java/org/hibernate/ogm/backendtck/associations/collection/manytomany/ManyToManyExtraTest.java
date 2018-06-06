/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.collection.manytomany;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfAssociations;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfEntities;

import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.After;
import org.junit.Test;

/**
 * Verify cardinality of entities and associations
 * in unidirectional {@link javax.persistence.OneToMany}.
 *
 * Rule of thumb:
 * Every {@link javax.persistence.Entity} instance count as 1 entity.
 * Every {@link org.hibernate.mapping.Bag} mapping an association, present on any {@link javax.persistence.Entity}, counts as 1 association.
 * If the bag is empty it counts as 0 association!
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Fabio Massimo Ercoli &lt;fabio@hibernate.org&gt;
 */
public class ManyToManyExtraTest extends OgmTestCase {

	@After
	public void cleanUp() {
		deleteAll( ClassRoom.class, 1L, 2L, 3L );
		deleteAll( Student.class, "john", "kate", "mario" );
		checkCleanCache();
	}

	@Test
	public void testUnidirectionalManyToMany2Entities1Association() {
		Student john = new Student( "john", "John Doe" );

		inTransaction( session -> {
			ClassRoom math = new ClassRoom( 1L, "Math" );
			math.getStudents().add( john );

			persistAll( session, math, john );
		} );

		inTransaction( session -> {
			ClassRoom math = session.load( ClassRoom.class, 1L );

			assertThat( math ).isNotNull();

			assertThat( math.getStudents() ).containsOnly( john );
		} );

		assertThat( getNumberOfEntities( sessionFactory ) ).isEqualTo( 2 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( 1 );
	}

	@Test
	public void testUnidirectionalManyToMany3Entities1Association() {
		Student john = new Student( "john", "John Doe" );
		Student kate = new Student( "kate", "Kate Doe" );

		inTransaction( session -> {
			ClassRoom math = new ClassRoom( 1L, "Math" );
			math.getStudents().add( john );
			math.getStudents().add( kate );

			persistAll( session, math, john, kate );
		} );

		inTransaction( session -> {
			ClassRoom math = session.load( ClassRoom.class, 1L );

			assertThat( math ).isNotNull();

			assertThat( math.getStudents() ).containsOnly( john, kate );
		} );

		assertThat( getNumberOfEntities( sessionFactory ) ).isEqualTo( 3 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( 1 );
	}

	@Test
	public void testUnidirectionalManyToMany4Entities1Association() {
		Student john = new Student( "john", "John Doe" );
		Student kate = new Student( "kate", "Kate Doe" );

		inTransaction( session -> {
			ClassRoom math = new ClassRoom( 1L, "Math" );
			math.getStudents().add( john );

			// no student classRoom
			ClassRoom english = new ClassRoom( 2L, "English" );

			persistAll( session, math, english, john, kate );
		} );

		inTransaction( session -> {
			ClassRoom math = session.load( ClassRoom.class, 1L );
			ClassRoom english = session.load( ClassRoom.class, 2L );

			assertThat( math ).isNotNull();
			assertThat( english ).isNotNull();

			assertThat( math.getStudents() ).containsOnly( john );
			assertThat( english.getStudents() ).isEmpty();
		} );

		assertThat( getNumberOfEntities( sessionFactory ) ).isEqualTo( 4 );
		// if the association bag is empty it counts as 0
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( 1 );
	}

	@Test
	public void testUnidirectionalManyToMany4Entities2Association() {
		Student john = new Student( "john", "John Doe" );
		Student kate = new Student( "kate", "Kate Doe" );

		inTransaction( session -> {
			ClassRoom math = new ClassRoom( 1L, "Math" );
			math.getStudents().add( john );

			ClassRoom english = new ClassRoom( 2L, "English" );
			english.getStudents().add( kate );

			persistAll( session, math, english, john, kate );
		} );

		inTransaction( session -> {
			ClassRoom math = session.load( ClassRoom.class, 1L );
			ClassRoom english = session.load( ClassRoom.class, 2L );

			assertThat( math ).isNotNull();
			assertThat( english ).isNotNull();

			assertThat( math.getStudents() ).containsOnly( john );
			assertThat( english.getStudents() ).containsOnly( kate );
		} );

		assertThat( getNumberOfEntities( sessionFactory ) ).isEqualTo( 4 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( 2 );
	}

	@Test
	public void testUnidirectionalManyToMany5Entities2Association() {
		Student john = new Student( "john", "John Doe" );
		Student kate = new Student( "kate", "Kate Doe" );
		Student mario = new Student( "mario", "Mario Rossi" );

		inTransaction( session -> {
			ClassRoom math = new ClassRoom( 1L, "Math" );
			math.getStudents().add( john );
			math.getStudents().add( mario );
			ClassRoom english = new ClassRoom( 2L, "English" );
			english.getStudents().add( kate );
			english.getStudents().add( mario );

			persistAll( session, math, english, john, mario, kate );
		} );

		inTransaction( session -> {
			ClassRoom math = session.load( ClassRoom.class, 1L );
			ClassRoom english = session.load( ClassRoom.class, 2L );

			assertThat( math ).isNotNull();
			assertThat( english ).isNotNull();

			assertThat( math.getStudents() ).containsOnly( john, mario );
			assertThat( english.getStudents() ).containsOnly( kate, mario );
		} );

		assertThat( getNumberOfEntities( sessionFactory ) ).isEqualTo( 5 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( 2 );
	}

	@Test
	public void testUnidirectionalManyToMany6Entities3Association() {
		Student john = new Student( "john", "John Doe" );
		Student kate = new Student( "kate", "Kate Doe" );
		Student mario = new Student( "mario", "Mario Rossi" );

		inTransaction( session -> {
			ClassRoom math = new ClassRoom( 1L, "Math" );
			math.getStudents().add( john );
			ClassRoom english = new ClassRoom( 2L, "English" );
			english.getStudents().add( kate );
			ClassRoom geology = new ClassRoom( 3L, "Geology" );
			geology.getStudents().add( mario );

			persistAll( session, math, english, geology, john, mario, kate );
		} );

		inTransaction( session -> {
			ClassRoom math = session.load( ClassRoom.class, 1L );
			ClassRoom english = session.load( ClassRoom.class, 2L );
			ClassRoom geology = session.load( ClassRoom.class, 3L );

			assertThat( math ).isNotNull();
			assertThat( english ).isNotNull();
			assertThat( geology ).isNotNull();

			assertThat( math.getStudents() ).containsOnly( john );
			assertThat( english.getStudents() ).containsOnly( kate );
			assertThat( geology.getStudents() ).containsOnly( mario );
		} );

		assertThat( getNumberOfEntities( sessionFactory ) ).isEqualTo( 6 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( 3 );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Student.class,
				ClassRoom.class
		};
	}
}
