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

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@SkipByGridDialect(
		value = { GridDialectType.INFINISPAN_REMOTE },
		comment = "Classroom.students list - bag semantics unsupported (no primary key)"
)
public class ManyToManyExtraTest extends OgmTestCase {

	@Test
	public void testUnidirectionalManyToMany() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		Student john = new Student( "john", "John Doe" );
		Student kate = new Student( "kate", "Kate Doe" );
		Student mario = new Student( "mario", "Mario Rossi" );

		ClassRoom math = new ClassRoom( 1L, "Math" );
		math.getStudents().add( john );
		math.getStudents().add( mario );
		ClassRoom english = new ClassRoom( 2L, "English" );
		english.getStudents().add( kate );
		english.getStudents().add( mario );

		persist( session, math, english, john, mario, kate );
		tx.commit();

		assertThat( getNumberOfEntities( sessionFactory ) ).isEqualTo( 5 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).isEqualTo( 2 );
		session.clear();

		delete( session, math, english, john, mario, kate );

		session.close();
		checkCleanCache();
	}

	private void persist(Session session, Object... entities) {
		for ( Object entity : entities ) {
			session.persist( entity );
		}
	}

	private void delete(Session session, Object... entities) {
		Transaction transaction = session.beginTransaction();
		for ( Object entity : entities ) {
			session.delete( entity );
		}
		transaction.commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Student.class,
				ClassRoom.class
		};
	}
}
