/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.dropSchemaAndDatabase;
import static org.hibernate.ogm.utils.jpa.JpaTestCase.extractJBossTransactionManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

import org.hibernate.ogm.backendtck.simpleentity.Hero;
import org.hibernate.ogm.backendtck.simpleentity.SuperHero;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Jonathan Wood <jonathanshawwood@gmail.com>
 */
public class JPAPolymorphicCollectionTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone.xml", Hero.class,
			SuperHero.class, HeroClub.class );

	@Test
	public void testJPAPolymorphicCollection() throws Exception {

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
		HeroClub hc = new HeroClub();
		hc.setName( "My hero club" );
		hc.getMembers().add( h );
		hc.getMembers().add( sh );
		em.persist( hc );
		transactionManager.commit();

		em.clear();

		transactionManager.begin();
		HeroClub lhc = em.find( HeroClub.class, hc.getName() );
		assertThat( lhc ).isNotNull();
		Hero lh = lhc.getMembers().get( 0 );
		assertThat( lh ).isNotNull();
		assertThat( lh ).isInstanceOf( Hero.class );
		Hero lsh = lhc.getMembers().get( 1 );
		assertThat( lsh ).isNotNull();
		assertThat( lsh ).isInstanceOf( SuperHero.class );
		lhc.getMembers().clear();
		em.remove( lh );
		em.remove( lsh );
		em.remove( lhc );

		transactionManager.commit();

		em.close();

		dropSchemaAndDatabase( emf );
		emf.close();
	}

}
