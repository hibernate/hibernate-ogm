package org.hibernate.ogm.test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class Utils {
	public static EntityManager getEM(){
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("mongoPU");
		return emf.createEntityManager();
	}
}
