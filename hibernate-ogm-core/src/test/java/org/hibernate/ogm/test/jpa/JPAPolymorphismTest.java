package org.hibernate.ogm.test.jpa;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.TestHelper.dropSchemaAndDatabase;
import static org.hibernate.ogm.test.utils.jpa.JpaTestCase.extractJBossTransactionManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

import org.hibernate.ogm.test.simpleentity.Hero;
import org.hibernate.ogm.test.simpleentity.SuperHero;
import org.hibernate.ogm.test.utils.PackagingRule;
import org.hibernate.ogm.test.utils.TestHelper;
import org.junit.Rule;
import org.junit.Test;

public class JPAPolymorphismTest {
	
	

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone.xml", Hero.class, SuperHero.class );

	@Test
	public void testJPAPolymorph() throws Exception {

		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone", TestHelper.getEnvironmentProperties() );

		TransactionManager transactionManager = extractJBossTransactionManager( emf );

		transactionManager.begin();
		final EntityManager em = emf.createEntityManager();
		Hero h = new Hero();
		h.setName( "Spartacus" );
		em.persist( h );
		SuperHero sh = new SuperHero();
		sh.setName( "Batman" );
		sh.setSpecialPower( "Technology and samurai techniques" );
		em.persist( sh );
		transactionManager.commit();

		em.clear();
		
		transactionManager.begin();
		Hero lh = em.find( Hero.class, h.getName() );
		assertThat( lh ).isNotNull();
		assertThat( h.getName() ).isEqualTo( lh.getName() );
		Hero lsh = em.find( Hero.class, sh.getName() );
		assertThat( lsh ).isNotNull();
		assertThat( lsh ).isInstanceOf(SuperHero.class);
		assertThat( sh.getSpecialPower() ).isEqualTo( ((SuperHero)lsh).getSpecialPower() );
		em.remove( lh );
		em.remove( lsh );

		transactionManager.commit();

		em.close();

		dropSchemaAndDatabase( emf );
		emf.close();
	}

}
