package org.hibernate.ogm.test.associations.embedded;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.ogm.test.Utils;

import org.hibernate.ogm.test.Utils;
import org.hibernate.ogm.test.associations.embedded.entities.EmbeddedObject;
import org.hibernate.ogm.test.associations.embedded.entities.EmbeddedSecondLvl;
import org.hibernate.ogm.test.associations.embedded.entities.Root;
import org.junit.Test;

public class CrudTest {

	@Test
	public void persistTest() {
		EntityManager em = Utils.getEM();

		String rootValue = "Root object";
		String embValue = "First Embedded";
		int embIntValue = 10;
		String secondValue = "secondLevel";

		String id = this.createDefaultObject( rootValue, embValue, embIntValue, secondValue );

		Root checkRoot = em.find( Root.class, id );
		assertNotNull( checkRoot );
		assertEquals( checkRoot.getRootValue(), rootValue );
		assertNotNull( checkRoot.getEmbeddedObject() );
		EmbeddedObject checkEmb = checkRoot.getEmbeddedObject();
		assertEquals( checkEmb.getEmbValue(), embValue );
		assertEquals( checkEmb.getIntValue(), embIntValue );
		assertNotNull( checkEmb.getSecondLvl() );
		EmbeddedSecondLvl secondCheck = checkEmb.getSecondLvl();
		assertEquals( secondCheck.getSecondLvlValue(), secondValue );

		em.close();

	}

	private String createDefaultObject(String rootValue, String embValue, int embIntValue, String secondValue) {
		EntityManager em = Utils.getEM();
		EmbeddedSecondLvl esl = new EmbeddedSecondLvl( secondValue );
		EmbeddedObject emb = new EmbeddedObject( embValue, embIntValue, esl );
		Root root = new Root( rootValue, emb );

		EntityTransaction tx = em.getTransaction();
		tx.begin();
		em.persist( root );
		em.flush();
		String id = root.getId();
		tx.commit();
		em.close();
		return id;
	}

	@Test
	public void deleteTest() {
		EntityManager em = Utils.getEM();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		String id = this.createDefaultObject( "rootValue", "embValue", 100, "second" );
		em.remove( em.find( Root.class, id ) );
		em.flush();
		tx.commit();
		assertNull( em.find( Root.class, id ) );
		em.close();
	}

	@Test
	public void updateFirstLevelTest() {
		String beforeUpdateValue = "rootValue";
		String id = this.createDefaultObject( beforeUpdateValue, "embValue", 100, "second" );

		EntityManager em = Utils.getEM();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		Root root = em.find( Root.class, id );
		assertEquals( root.getRootValue(), beforeUpdateValue );

		String newRootValue = "new Value";
		root.setRootValue( newRootValue );
		em.merge( root );
		em.flush();
		tx.commit();

		Root checkRoot = em.find( Root.class, id );
		assertEquals( checkRoot.getRootValue(), newRootValue );
		em.close();
	}

	@Test
	public void updateSecondLevelTest() {
		String beforeUpdateValue = "embValue";
		int beforeIntValue = 100;

		String id = this.createDefaultObject( "rootValue", beforeUpdateValue, beforeIntValue, "second" );

		EntityManager em = Utils.getEM();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		Root root = em.find( Root.class, id );
		assertEquals( root.getEmbeddedObject().getEmbValue(), beforeUpdateValue );
		assertEquals( root.getEmbeddedObject().getIntValue(), beforeIntValue );

		String newSecondValue = "new Value";
		int newIntValue = 200;
		root.getEmbeddedObject().setEmbValue( newSecondValue );
		root.getEmbeddedObject().setIntValue( newIntValue );
		em.merge( root );
		em.flush();
		tx.commit();

		Root checkRoot = em.find( Root.class, id );
		assertEquals( checkRoot.getEmbeddedObject().getEmbValue(), newSecondValue );
		assertEquals( checkRoot.getEmbeddedObject().getIntValue(), newIntValue );
		em.close();
	}

	@Test
	public void updateThirdLevelTest() {
		String beforeUpdateValue = "thirdBefore";
		String id = this.createDefaultObject( "rootValue", "secondLevel", 10, beforeUpdateValue );

		EntityManager em = Utils.getEM();
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		Root root = em.find( Root.class, id );

		String newThirdValue = "new Value";
		root.getEmbeddedObject().getSecondLvl().setSecondLvlValue( newThirdValue );
		em.merge( root );
		em.flush();
		tx.commit();

		Root checkRoot = em.find( Root.class, id );
		assertEquals( checkRoot.getEmbeddedObject().getSecondLvl().getSecondLvlValue(), newThirdValue );
		em.close();
	}
}
