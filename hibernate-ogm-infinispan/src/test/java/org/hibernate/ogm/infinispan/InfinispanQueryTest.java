package org.hibernate.ogm.infinispan;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.ogm.cfg.OgmConfiguration;

@SuppressWarnings("nls")
public class InfinispanQueryTest {

	public void testSimpleCRUD() throws Exception {
		final Session session = getSession();

		Transaction transaction = session.beginTransaction();
		for (int i = 0; i < 100; i++) {
			G1 g1 = new G1(i, "foo"+i);
			session.persist(g1);
			G2 g2 = new G2(i, "bar"+i);
			g2.setG1(g1);
			session.persist(g2);
			g1.setG2(g2);
			session.persist(g1);
		}
		transaction.commit();
		
		// key search
		transaction = session.beginTransaction();
		G1 g1 = (G1)session.get(G1.class, 23);
		assertTrue(g1.getE2().equals("foo23"));
		transaction.commit();
		
		System.out.println("from G1 where e2='foo23'");
		// non key search
		transaction = session.beginTransaction();
		List<G1> g1Rows = session.createQuery("from G1 where e2='foo23'").list();
		transaction.commit();
		
		for (G1 row: g1Rows) {
			System.out.println("g1.e1="+row.getE1()+", g1.e2="+row.getE2());
		}

		System.out.println("select g1.e1, g2.e2 from G1 g1 join g1.g2 g2 where g1.e1 in (1,2,3,4,5) order by g1.e1");
		
		// inner join
		transaction = session.beginTransaction();
		List<Object[]> rows = session.createQuery("select g1.e1, g2.e2 from G1 g1 join g1.g2 g2 where g1.e1 in (1,2,3,4,5) order by g1.e1").list();
		transaction.commit();
		
		for (Object[] obj: rows) { 
			System.out.println(obj[0] +","+ obj[1]);
		}
		
		System.out.println("delete");
		// delete
		transaction = session.beginTransaction();
		g1 = (G1)session.get(G1.class, 1);
		session.delete(g1);
		transaction.commit();
		
		System.out.println("delete-check");
		// check 
		transaction = session.beginTransaction();
		g1 = (G1)session.get(G1.class, 1);
		if (g1 == null) {
			System.out.println("Did not find it");
		}
		transaction.commit();		
		
		
		session.close();
	}
	
	private Session getSession() {
		SessionFactory sessionFactory = new OgmConfiguration()
		.setProperty( "hibernate.ogm.datastore.provider", "infinispan" )
		.setProperty( "hibernate.ogm.infinispan.configuration_resourcename", "infinispan-local.xml")
		.setProperty( "hibernate.ogm.datastore.grid_translator", "org.hibernate.ogm.dialect.infinispan.InfinispanGridExecutionFactory")
		.addAnnotatedClass(G1.class)
		.addAnnotatedClass(G2.class)
		.buildSessionFactory();		
		return sessionFactory.openSession();
	}
	
	public static void main(String[] args) throws Exception {
		InfinispanQueryTest test = new InfinispanQueryTest();
		test.testSimpleCRUD();
	}
}
