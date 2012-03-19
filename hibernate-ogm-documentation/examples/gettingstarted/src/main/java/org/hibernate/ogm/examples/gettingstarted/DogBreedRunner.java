package org.hibernate.ogm.examples.gettingstarted;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

import org.hibernate.ogm.examples.gettingstarted.domain.Breed;
import org.hibernate.ogm.examples.gettingstarted.domain.Dog;
import org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DogBreedRunner {
	
	private static final Logger logger = LoggerFactory.getLogger(DogBreedRunner.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//accessing JBoss's Transaction can be done differently but this one works nicely
		TransactionManager tm = new JBossStandAloneJtaPlatform().getTransactionManager();

		//build the EntityManagerFactory as you would build in in Hibernate Core
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("org.hibernate.ogm.tutorial.jpa");

		//Persist entities the way you are used to in plain JPA
		try {
			tm.begin();
			EntityManager em = emf.createEntityManager();
			Breed collie = new Breed();
			collie.setName("Collie");
			em.persist(collie);
			Dog dina = new Dog();
			dina.setName("Dina");
			dina.setBreed(collie);
			em.persist(dina);
			Long dinaId = dina.getId();
			em.flush();
			em.close();
			tm.commit();

			//Retrieve your entities the way you are used to in plain JPA
			tm.begin();
			em = emf.createEntityManager();
			dina = em.find(Dog.class, dinaId);
			logger.debug(dina.getName());
			logger.debug(dina.getBreed().getName());
			em.flush();
			em.close();
			tm.commit();

			emf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
