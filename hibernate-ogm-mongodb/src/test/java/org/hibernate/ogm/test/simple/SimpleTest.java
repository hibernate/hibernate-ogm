package org.hibernate.ogm.test.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.junit.Test;

public class SimpleTest {

	@Test
	public void launchTest() {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("mongoPU");
		EntityManager em = emf.createEntityManager();
		assertNotNull(em);
		em.close();
	}

	@Test
	public void persistTest() {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("mongoPU");
		EntityManager em = emf.createEntityManager();
		TestModel tm = new TestModel("newTM");
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		em.persist(tm);
		em.flush();
		tx.commit();
		em.close();

	}

	@Test
	public void updateTest() {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("mongoPU");
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		TestModel updateTM = new TestModel("updateTMOld");
		em.persist(updateTM);
		em.flush();

		String id = updateTM.getId();
		String newProperty = "updateTMNew";
		updateTM.setName(newProperty);
		em.merge(updateTM);
		em.flush();

		TestModel toCheck = em.find(TestModel.class, id);
		assertEquals(newProperty, toCheck.getName());
		tx.commit();
		em.close();
	}

	@Test
	public void deleteTest() {
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("mongoPU");
		EntityManager em = emf.createEntityManager();
		EntityTransaction tx = em.getTransaction();
		tx.begin();

		TestModel newTM = new TestModel("toDelete");
		em.persist(newTM);
		em.flush();

		String id = newTM.getId();
		TestModel toDelete = em.find(TestModel.class, id);
		em.remove(toDelete);
		em.flush();

		TestModel toCheck = em.find(TestModel.class, id);
		assertNull(toCheck);
		tx.commit();
		em.close();
	}
}
