/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.simpleentity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Nicolas Helleringer
 */
public class CRUDTest extends OgmTestCase {

	@Test
	public void testSimpleCRUD() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();
		Hypothesis hyp = new Hypothesis();
		hyp.setId( "1234567890" );
		hyp.setDescription( "NP != P" );
		hyp.setPosition( 1 );
		session.persist( hyp );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		final Hypothesis loadedHyp = (Hypothesis) session.get( Hypothesis.class, hyp.getId() );
		assertNotNull( "Cannot load persisted object", loadedHyp );
		assertEquals( "persist and load fails", hyp.getDescription(), loadedHyp.getDescription() );
		assertEquals( "@Column fails", hyp.getPosition(), loadedHyp.getPosition() );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		loadedHyp.setDescription( "P != NP");
		session.merge( loadedHyp );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		Hypothesis secondLoadedHyp = (Hypothesis) session.get( Hypothesis.class, hyp.getId() );
		assertEquals( "Merge fails", loadedHyp.getDescription(), secondLoadedHyp.getDescription() );
		session.delete( secondLoadedHyp );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		assertNull( session.get( Hypothesis.class, hyp.getId() ) );
		transaction.commit();

		session.close();
	}

	private void performanceLoop() throws Exception {
		long start = 0;
		for ( int i = 0; i < Integer.MAX_VALUE; i++ ) {
			if ( i % 10000 == 0 ) {
				start = System.nanoTime();
			}
			testSimpleCRUD();
			if ( i % 10000 == 9999 ) {
				long elapsed = System.nanoTime() - start;
				System.out.printf( "%.3E ms for 10000 tests\n", (elapsed) / 1000000f );
			}
		}
	}

	public static void main(String[] args) throws Exception {
		CRUDTest test = new CRUDTest();

		test.sessions = TestHelper
				.getDefaultTestConfiguration( test.getAnnotatedClasses() )
				.buildSessionFactory();

		try {
			test.performanceLoop();
		}
		finally {
			test.sessions.close();
		}
	}

	@Test
	public void testGeneratedValue() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();
		Helicopter h = new Helicopter();
		h.setName( "Eurocopter" );
		session.persist( h );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		h = (Helicopter) session.get( Helicopter.class, h.getUUID() );
		session.delete( h );
		transaction.commit();

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Hypothesis.class,
				Helicopter.class
		};
	}
}
