package org.hibernate.ogm.test.simpleentity;

import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * @author Emmanuel Bernard
 */
public class InheritanceTest extends OgmTestCase {

	public void testInheritance() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();
		Hero h = new Hero();
		h.setName( "Spartacus" );
		session.persist( h );
		SuperHero sh = new SuperHero();
		sh.setName( "Batman" );
		sh.setSpecialPower( "Technology and samurai techniques" );
		session.persist( sh );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		Hero lh = (Hero) session.get( Hero.class, h.getName() );
		assertNotNull( lh );
		assertEquals( h.getName(), lh.getName() );
		SuperHero lsh = (SuperHero) session.get( SuperHero.class, sh.getName() );
		assertNotNull( lsh );
		assertEquals( sh.getSpecialPower(), lsh.getSpecialPower() );
		session.delete( lh );
		session.delete( lsh );
		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Hero.class,
				SuperHero.class
		};
	}
}
