package org.hibernate.ogm.test.jpa;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.ogm.test.jpa.util.JpaTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test that PersistenceProvider#createContainerEntityManagerFactory work properly in a JTA environment
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				Poem.class
		};
	}
}
