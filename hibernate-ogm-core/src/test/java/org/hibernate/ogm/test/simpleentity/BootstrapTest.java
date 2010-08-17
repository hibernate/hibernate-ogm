package org.hibernate.ogm.test.simpleentity;

import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * @author Emmanuel Bernard
 */
public class BootstrapTest extends OgmTestCase {

	public void testBootstrap() throws Exception {
		final Session session = openSession();
		Hypothesis hyp = new Hypothesis();
		hyp.setId( "1234567890" );
		hyp.setDescription( "P != NP" );
		final Transaction transaction = session.beginTransaction();
		session.persist( hyp );
		session.flush();
		session.clear();
		final Hypothesis loadedHyp = (Hypothesis) session.get( Hypothesis.class, hyp.getId() );
		assertEquals( hyp.getDescription(), loadedHyp.getDescription() );
		transaction.rollback();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Hypothesis.class
		};
	}
}
