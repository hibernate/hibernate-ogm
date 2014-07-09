/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.hibernate.ogm.utils.jpa.JpaTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test case for JPA Auto identifier generator.
 *
 * @author Nabeel Ali Memon &lt;nabeel@nabeelalimemon.com&gt;
 */
public class AutoIdGeneratorTest extends JpaTestCase {

	@Test
	public void testAutoIdentifierGenerator() throws Exception {
		DistributedRevisionControl git = new DistributedRevisionControl();
		DistributedRevisionControl bzr = new DistributedRevisionControl();
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		boolean operationSuccessfull = false;
		try {
			git.setName( "Git" );
			em.persist( git );

			bzr.setName( "Bazaar" );
			em.persist( bzr );
			operationSuccessfull = true;
		}
		finally {
			commitOrRollback( operationSuccessfull );
		}

		em.clear();
		getTransactionManager().begin();
		operationSuccessfull = false;
		try {
			DistributedRevisionControl dvcs = em.find( DistributedRevisionControl.class, git.getId() );
			assertThat( dvcs ).isNotNull();
			assertThat( dvcs.getId() ).isEqualTo( 1 );
			em.remove( dvcs );

			dvcs = em.find( DistributedRevisionControl.class, bzr.getId() );
			assertThat( dvcs ).isNotNull();
			assertThat( dvcs.getId() ).isEqualTo( 2 );
			operationSuccessfull = true;
		}
		finally {
			commitOrRollback( operationSuccessfull );
		}
		em.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				DistributedRevisionControl.class
		};
	}
}
