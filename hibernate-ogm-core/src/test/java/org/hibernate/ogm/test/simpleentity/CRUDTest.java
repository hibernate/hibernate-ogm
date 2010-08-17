package org.hibernate.ogm.test.simpleentity;

import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * @author Emmanuel Bernard
 */
public class CRUDTest extends OgmTestCase {

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
