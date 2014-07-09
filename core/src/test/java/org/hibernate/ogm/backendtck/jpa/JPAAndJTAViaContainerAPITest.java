/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.jpa;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.hibernate.ogm.utils.jpa.JpaTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test that PersistenceProvider#createContainerEntityManagerFactory work properly in a JTA environment
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class JPAAndJTAViaContainerAPITest extends JpaTestCase {
	@Test
	public void doTest() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		Poem poem = new Poem();
		poem.setName( "L'albatros" );
		em.persist( poem );
		getTransactionManager().commit();

		em.clear();

		getTransactionManager().begin();
		poem = em.find( Poem.class, poem.getId() );
		assertThat( poem ).isNotNull();
		assertThat( poem.getName() ).isEqualTo( "L'albatros" );
		em.remove( poem );
		getTransactionManager().commit();

		em.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				Poem.class
		};
	}
}
