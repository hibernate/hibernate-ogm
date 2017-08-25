/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.jpa;

import static org.fest.assertions.Assertions.assertThat;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactoryImpl;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionImpl;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class JPAAPIWrappingTest extends OgmJpaTestCase {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/ogm.xml", Poem.class );

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testWrappedStandalone() throws Exception {
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "ogm", TestHelper.getDefaultTestSettings() );
		assertThat( emf.getClass() ).isEqualTo( OgmSessionFactoryImpl.class );

		EntityManager em = emf.createEntityManager();
		assertThat( em.getClass() ).isEqualTo( OgmSessionImpl.class );
		em.close();

		em = emf.createEntityManager();
		assertThat( em.getClass() ).isEqualTo( OgmSessionImpl.class );
		em.close();

		emf.close();
	}

	@Test
	public void testUndefinedPU() throws Exception {
		thrown.expect( PersistenceException.class );
		Persistence.createEntityManagerFactory( "does-not-exist-PU" );
	}

	@Test
	public void testWrapInContainer() throws Exception {
		assertThat( getFactory().getClass() ).isEqualTo( OgmSessionFactoryImpl.class );
		EntityManager entityManager = getFactory().createEntityManager();
		assertThat( entityManager.getClass() ).isEqualTo( OgmSessionImpl.class );
		entityManager.close();
		entityManager = getFactory().createEntityManager();
		assertThat( entityManager.getClass() ).isEqualTo( OgmSessionImpl.class );
		entityManager.close();
	}

	@Test
	public void testIllegalArgumentExceptionIfQueryDefinitionDoesNotExists() throws Exception {
		thrown.expect( IllegalArgumentException.class );
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		try {
			em.createNamedQuery( "DoesNotExistsQuery" );
		}
		finally {
			em.getTransaction().rollback();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Poem.class };
	}
}
