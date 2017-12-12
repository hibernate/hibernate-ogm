/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.recursive;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Before;
import org.junit.Test;

/**
 * Create a linked list of trained coaches and test that the session.load returns the expected value.
 * <p>
 * This test has been introduced to check the query generated for Neo4j, but I left it because there wasn't a test for
 * bidirectional one-to-one associations on the same entity type.
 *
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "OGM-1344")
public class TrainCoachesTest extends OgmTestCase {

	/*
	 * The number of coaches of a train
	 */
	private static final int COACHES_NUM = 20;

	@Before
	public void setup() {
		persistTrain();
	}

	/*
	 * A train made of coaches numbered in order. Pretty much a simple linked list.
	 */
	private void persistTrain() {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			Coach previous = null;
			for ( int i = 0; i < COACHES_NUM; i++ ) {
				Coach coach = new Coach( i );
				if ( previous != null ) {
					previous.setNext( coach );
					coach.setPrevious( previous );
				}
				session.persist( coach );
				previous = coach;
			}
			tx.commit();
		}
	}

	@Test
	public void testLoad() {
		int coachId = COACHES_NUM / 2;
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			Coach coach = session.load( Coach.class, coachId );
			assertCoach( coachId, coach );
			assertCoach( coachId - 1, coach.getPrevious() );
			assertCoach( coachId + 1, coach.getNext() );
			tx.commit();
		}
	}

	private void assertCoach(int expectedCoachId, Coach coach) {
		assertThat( coach ).isNotNull();
		assertThat( coach.getNumber() ).isEqualTo( expectedCoachId );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Coach.class };
	}
}
