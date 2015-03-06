/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.hibernate.ogm.backendtck.simpleentity.Hero;
import org.hibernate.ogm.backendtck.simpleentity.SuperHero;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestHelper;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.dropSchemaAndDatabase;

/**
 * @author Jonathan Wood <jonathanshawwood@gmail.com>
 */
public class JPAPolymorphicCollectionTest {
	@Rule
	public PackagingRule packaging = new PackagingRule(
			"persistencexml/ogm.xml",
			Hero.class,
			SuperHero.class, HeroClub.class
	);

	private EntityManagerFactory emf;
	private EntityManager em;

	@Before
	public void setUp() {
		emf = Persistence.createEntityManagerFactory( "ogm",
				TestHelper.getEnvironmentProperties() );
		em = emf.createEntityManager();
	}

	@After
	public void tearDown() {
		dropSchemaAndDatabase( emf );
		em.close();
		emf.close();
	}

	@Test
	public void testJPAPolymorphicCollection() throws Exception {
		em.getTransaction().begin();
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
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
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
		em.getTransaction().commit();
	}

}
