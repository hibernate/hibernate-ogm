/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.examples.gettingstarted;

import org.hibernate.ogm.examples.gettingstarted.domain.Breed;
import org.hibernate.ogm.examples.gettingstarted.domain.Dog;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

public class DogBreedRunner {

	private static final Log logger = LoggerFactory.make( MethodHandles.lookup() );

	public static void main(String[] args) {

		TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();

		//build the EntityManagerFactory as you would build in in Hibernate Core
		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "ogm-jpa-tutorial" );

		//Persist entities the way you are used to in plain JPA
		try {
			tm.begin();
			logger.infof( "About to store dog and breed" );
			EntityManager em = emf.createEntityManager();
			Breed collie = new Breed();
			collie.setName( "Collie" );
			em.persist( collie );
			Dog dina = new Dog();
			dina.setName( "Dina" );
			dina.setBreed( collie );
			em.persist( dina );
			Long dinaId = dina.getId();
			em.flush();
			em.close();
			tm.commit();

			//Retrieve your entities the way you are used to in plain JPA
			logger.infof( "About to retrieve dog and breed" );
			tm.begin();
			em = emf.createEntityManager();
			dina = em.find( Dog.class, dinaId );
			logger.infof( "Found dog %s of breed %s", dina.getName(), dina.getBreed().getName() );
			em.flush();
			em.close();
			tm.commit();

			emf.close();
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}

	}

}
