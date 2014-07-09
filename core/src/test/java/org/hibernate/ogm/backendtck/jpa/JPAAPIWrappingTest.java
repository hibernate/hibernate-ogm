/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.jpa;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import org.hibernate.ogm.jpa.impl.OgmEntityManager;
import org.hibernate.ogm.jpa.impl.OgmEntityManagerFactory;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class JPAAPIWrappingTest extends JpaTestCase {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone.xml", Poem.class );

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testWrappedStandalone() throws Exception {
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone", TestHelper.getEnvironmentProperties() );
		assertThat( emf.getClass() ).isEqualTo( OgmEntityManagerFactory.class );

		EntityManager em = emf.createEntityManager();
		assertThat( em.getClass() ).isEqualTo( OgmEntityManager.class );
		em.close();

		em = emf.createEntityManager( new HashMap() );
		assertThat( em.getClass() ).isEqualTo( OgmEntityManager.class );
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
		assertThat( getFactory().getClass() ).isEqualTo( OgmEntityManagerFactory.class );
		EntityManager entityManager = getFactory().createEntityManager();
		assertThat( entityManager.getClass() ).isEqualTo( OgmEntityManager.class );
		entityManager.close();
		entityManager = getFactory().createEntityManager( new HashMap() );
		assertThat( entityManager.getClass() ).isEqualTo( OgmEntityManager.class );
		entityManager.close();
	}

	@Test
	public void testIllegalArgumentExceptionIfQueryDefinitionDoesNotExists() throws Exception {
		thrown.expect( IllegalArgumentException.class );
		EntityManager em = getFactory().createEntityManager();
		em.createNamedQuery( "DoesNotExistsQuery" );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { Poem.class };
	}
}
