package org.hibernate.ogm.datastore.ignite.test;

import java.io.Serializable;

import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.ignite.impl.IgniteSessionImpl;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.Assert;

public abstract class BaseIgniteTest extends OgmTestCase {

	@Override
	protected OgmSession openSession() {
		OgmSession session = super.openSession();
		Assert.assertEquals(IgniteSessionImpl.class, session.getClass());
		return session;
	}
	
	public Object testGet(Class<?> clazz, Serializable id) throws Exception {
		OgmSession session = openSession();
		Object result = session.get(clazz, id);
		session.close();
		return result;
	}
	
	public void testInsert(OgmSession session, Object object) throws Exception {
		boolean active = session.getTransaction().getStatus() == TransactionStatus.ACTIVE;
		if (!active)
			session.getTransaction().begin();
		session.persist(object);
		if (!active)
			session.getTransaction().commit();
	}
	
	public void testUpdateNewSession(Object object){
		OgmSession session = null;
		try {
			session = openSession();
			session.getTransaction().begin();
			session.saveOrUpdate(object);
			session.getTransaction().commit();
		}
		finally {
			if (session != null)
				session.close();
		}
	}
	
	public void testUpdate(OgmSession session, Object object) {
		boolean active = session.getTransaction().getStatus() == TransactionStatus.ACTIVE;
		if (!active)
			session.getTransaction().begin();
		session.saveOrUpdate(object);
		if (!active)
			session.getTransaction().commit();
	}
	
	public void testRemove(OgmSession session, Object object) throws Exception {
		boolean active = session.getTransaction().getStatus() == TransactionStatus.ACTIVE;
		if (!active)
			session.getTransaction().begin();
		session.delete(object);
		if (!active)
			session.getTransaction().commit();
	}

}
