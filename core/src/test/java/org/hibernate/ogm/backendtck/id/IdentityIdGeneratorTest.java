/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import static org.fest.assertions.Assertions.assertThat;

import javax.persistence.EntityManager;

import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.Test;

/**
 * @author Nabeel Ali Memon <nabeel@nabeelalimemon.com>
 */
public class IdentityIdGeneratorTest extends JpaTestCase {

	@Test
	public void testIdentityGenerator() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		Animal jungleKing = new Animal();
		Animal fish = new Animal();
		boolean ok = false;
		try {
			jungleKing.setName( "Lion" );
			jungleKing.setSpecies( "Mammal" );
			em.persist( jungleKing );

			fish.setName( "Shark" );
			fish.setSpecies( "Tiger Shark" );
			em.persist( fish );
			ok = true;
		}
		finally {
			commitOrRollback( ok );
		}
		em.clear();

		getTransactionManager().begin();
		ok = false;
		try {
			Animal animal = em.find( Animal.class, jungleKing.getId() );
			assertThat( animal ).isNotNull();
			assertThat( animal.getId() ).isEqualTo( 1 );
			assertThat( animal.getName() ).isEqualTo( "Lion" );
			em.remove( animal );

			animal = em.find( Animal.class, fish.getId() );
			assertThat( animal ).isNotNull();
			assertThat( animal.getId() ).isEqualTo( 2 );
			assertThat( animal.getName() ).isEqualTo( "Shark" );
			em.remove( animal );
			ok = true;
		}
		finally {
			commitOrRollback( ok );
		}
		em.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				Animal.class
		};
	}
}
