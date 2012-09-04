package org.hibernate.ogm.example;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.ogm.teiid.TeiidOgmConfiguration;

@SuppressWarnings("nls")
public class DialectTest {

	public void testSimpleCRUD() throws Exception {
		final Session session = getSession();

		Transaction transaction = session.beginTransaction();
		for (int i = 0; i < 100; i++) {
			G1 g1 = new G1(i, "foo"+i);
			G2 g2 = new G2(i, "bar"+i);
			g1.setG2(g2);
			g2.setG1(g1);
			session.persist(g1);
			session.persist(g2);
		}
		transaction.commit();
		
		// key search
		transaction = session.beginTransaction();
		G1 g1= (G1)session.get(G1.class, 23);
		if (g1.getE1() == 23) {
			System.out.println("*********found it");
		}
		transaction.commit();
		
		// non key search
		transaction = session.beginTransaction();
		List<G1> g1Rows = session.createQuery("from G1 where e2='foo23'").list();
		transaction.commit();
		
		for (G1 row: g1Rows) {
			System.out.println("g1.e1="+row.getE1()+", g1.e2="+row.getE2());
		}
		
		// inner join
		transaction = session.beginTransaction();
		List<Object[]> rows = session.createQuery("select g1.e1, g2.e2 from G1 g1 join g1.g2 g2 where g1.e1 in (1,2,3,4,5) order by g1.e1").list();
		transaction.commit();
		
		for (Object[] obj: rows) { 
			System.out.println(obj[0] +","+ obj[1]);
		}
		
		// delete
		transaction = session.beginTransaction();
		g1 = (G1)session.get(G1.class, 23);
		session.delete(g1);
		transaction.commit();
		
		// check 
		transaction = session.beginTransaction();
		g1 = (G1)session.get(G1.class, 23);
		if (g1 == null) {
			System.out.println("Did not find it");
		}
		transaction.commit();		
		
		
		session.close();
	}

	private Session getSession() {
		SessionFactory sessionFactory = new TeiidOgmConfiguration()
		.configure()
		.addAnnotatedClass(G1.class)
		.addAnnotatedClass(G2.class)
		.buildSessionFactory();		
		return sessionFactory.openSession();
	}
	
	public static void main(String[] args) throws Exception {
		DialectTest test = new DialectTest();
		test.testSimpleCRUD();
	}
}
