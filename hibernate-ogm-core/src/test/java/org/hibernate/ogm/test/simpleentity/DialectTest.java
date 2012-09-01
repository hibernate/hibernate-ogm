package org.hibernate.ogm.test.simpleentity;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.junit.Test;


public class DialectTest {

	@Test
	public void testSimpleCRUD() throws Exception {
		final Session session = getSession();

		Transaction transaction = session.beginTransaction();
		Hypothesis hyp = new Hypothesis();
		hyp.setId( "1234567890" );
		hyp.setDescription( "NP != P" );
		hyp.setPosition( 1 );
		session.persist( hyp );
		transaction.commit();

		session.close();
	}

	private Session getSession() {
		SessionFactory sessionFactory = new AnnotationConfiguration()
		.configure()
		.addPackage("org.hibernate.ogm.test.simpleentity") 
		.addAnnotatedClass(Hypothesis.class)
		.buildSessionFactory();		
		return sessionFactory.openSession();
	}
}
