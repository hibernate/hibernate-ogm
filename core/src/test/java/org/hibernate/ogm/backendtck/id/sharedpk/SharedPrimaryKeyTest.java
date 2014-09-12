/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id.sharedpk;

import static org.fest.assertions.Assertions.assertThat;

import javax.persistence.EntityManager;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.Test;

/**
 * Test for sharing the PK between two entities via {@link PrimaryKeyJoinColumn}.
 *
 * @author Gunnar Morling
 */
public class SharedPrimaryKeyTest extends JpaTestCase {
	@Test
	public void testSequenceIdGenerationInJTA() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		boolean operationSuccessful = false;

		CoffeeMug mug = null;

		// Persist mug and lid
		try {
			mug = new CoffeeMug();
			mug.setCapacity( 568 );
			Lid lid = new Lid();
			lid.setColor( "blue" );
			lid.setMug( mug );
			mug.setLid( lid );
			em.persist( mug );

			operationSuccessful = true;
		}
		finally {
			commitOrRollback( operationSuccessful );
		}
		em.clear();

		// load mug and lid; lid should inherit id from the mug
		getTransactionManager().begin();
		operationSuccessful = false;
		try {
			mug = em.find( CoffeeMug.class, mug.getId() );
			assertThat( mug ).isNotNull();
			assertThat( mug.getLid() ).isNotNull();
			assertThat( mug.getLid().getId() ).isEqualTo( mug.getId() );

			em.remove( mug );
			operationSuccessful = true;
		}
		finally {
			commitOrRollback( operationSuccessful );
		}

		em.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { CoffeeMug.class, Lid.class };
	}
}
