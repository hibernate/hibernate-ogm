/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class JpaQueriesTest extends OgmJpaTestCase {

	private static final String POLICE_HELICOPTER = "Bell 206";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private EntityManager em;

	@Test
	@SuppressWarnings("unchecked")
	public void testGetResultList() throws Exception {
		List<Helicopter> helicopters = em.createQuery( "FROM Helicopter WHERE name = :name" )
				.setParameter( "name", POLICE_HELICOPTER )
				.getResultList();

		assertThat( helicopters.size() ).isEqualTo( 1 );
		assertThat( helicopters.get( 0 ).getName() ).isEqualTo( POLICE_HELICOPTER );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGetResultListWithSelect() throws Exception {
		List<Helicopter> helicopters = em.createQuery( "SELECT h FROM Helicopter h WHERE h.name = :name" )
				.setParameter( "name", POLICE_HELICOPTER )
				.getResultList();

		assertThat( helicopters.size() ).isEqualTo( 1 );
		assertThat( helicopters.get( 0 ).getName() ).isEqualTo( POLICE_HELICOPTER );
	}

	@Test
	public void testGetResultListWithTypedQuery() throws Exception {
		List<Helicopter> helicopters = em.createQuery( "FROM Helicopter WHERE name = :name", Helicopter.class )
				.setParameter( "name", POLICE_HELICOPTER )
				.getResultList();

		assertThat( helicopters.size() ).isEqualTo( 1 );
		assertThat( helicopters.get( 0 ).getName() ).isEqualTo( POLICE_HELICOPTER );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testGetResultListSize() throws Exception {
		List<Helicopter> helicopters = em.createQuery( "FROM Helicopter" )
				.getResultList();

		assertThat( helicopters.size() ).isEqualTo( 2 );
	}

	@Test
	public void testGetResultListSizeWithTypedQuery() throws Exception {
		List<Helicopter> helicopters = em.createQuery( "FROM Helicopter", Helicopter.class )
				.getResultList();

		assertThat( helicopters.size() ).isEqualTo( 2 );
	}

	@Test( expected = IllegalArgumentException.class)
	public void testGetResultListSizeWithWrongReturnedClass() throws Exception {
		em.createQuery( "FROM Helicopter", Hypothesis.class );
	}

	@Test
	public void testSingleResult() throws Exception {
		Helicopter helicopter = (Helicopter) em.createQuery( "FROM Helicopter WHERE name = :name" )
				.setParameter( "name", POLICE_HELICOPTER )
				.getSingleResult();

		assertThat( helicopter.getName() ).isEqualTo( POLICE_HELICOPTER );
	}

	@Test
	public void testGetSingleResultTypedQuery() throws Exception {
		Helicopter helicopter = em.createQuery( "FROM Helicopter WHERE name = :name", Helicopter.class )
				.setParameter( "name", POLICE_HELICOPTER )
				.getSingleResult();

		assertThat( helicopter.getName() ).isEqualTo( POLICE_HELICOPTER );
	}

	@Test
	public void testCreateNamedQuery() throws Exception {
		Helicopter helicopter = (Helicopter) em.createNamedQuery( Helicopter.BY_NAME )
				.setParameter( "name", POLICE_HELICOPTER )
				.getSingleResult();

		assertThat( helicopter.getName() ).isEqualTo( POLICE_HELICOPTER );
	}

	@Test
	public void testCreateNamedQueryTypeQuery() throws Exception {
		Helicopter helicopter = em.createNamedQuery( Helicopter.BY_NAME, Helicopter.class )
				.setParameter( "name", POLICE_HELICOPTER )
				.getSingleResult();

		assertThat( helicopter.getName() ).isEqualTo( POLICE_HELICOPTER );
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateNamedQueryTypeQueryWithWrongReturnedClass() throws Exception {
		em.createNamedQuery( Helicopter.BY_NAME, Hypothesis.class )
			.setParameter( "name", POLICE_HELICOPTER )
			.getSingleResult();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testAddedNamedQuery() throws Exception {
		final String allHelicopters = "AllHelicopters";
		getFactory().addNamedQuery( allHelicopters, em.createQuery( "FROM Helicopter" ) );
		List<Helicopter> helicopters = em.createNamedQuery( allHelicopters ).getResultList();

		assertThat( helicopters.size() ).isEqualTo( 2 );
	}

	@Before
	public void populateDb() throws Exception {
		em = getFactory().createEntityManager();
		em.getTransaction().begin();
		em.persist( helicopter( POLICE_HELICOPTER ) );
		em.persist( helicopter( "AW139SAR"  ) );
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
	}

	@After
	public void closeEmAndRemoveEntities() throws Exception {
		//Do not hide the real cause with an NPE if there are initialization issues:
		if ( em != null ) {
			if ( em.getTransaction().isActive() ) {
				if ( em.getTransaction().getRollbackOnly() ) {
					em.getTransaction().rollback();
				}
				else {
					em.getTransaction().commit();
				}
			}
			em.close();
			removeEntities();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Helicopter.class };
	}

	private Helicopter helicopter(String name) {
		Helicopter helicopter = new Helicopter();
		helicopter.setName( name );
		return helicopter;
	}

}
