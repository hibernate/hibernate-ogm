/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.query.nativequery;

import static org.hibernate.ogm.utils.OgmAssertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteJpaServerRunner;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the execution of native queries on Infinispan Server using the {@link EntityManager}
 *
 * @author Fabio Massimo Ercoli &lt;fabio@hibernate.org&gt;
 */
@RunWith(InfinispanRemoteJpaServerRunner.class)
public class InfinispanRemoteEntityManagerNativeQueryTest extends OgmJpaTestCase {

	private final Employee john = new Employee( 1l, "John Doe", getDate( 2017, Month.JUNE, 12 ), 4 );
	private final Employee jane = new Employee( 2l, "Jane Gee", getDate( 2016, Month.MARCH, 7 ), 7 );
	private final Employee jake = new Employee( 3l, "Jake Dee", getDate( 2011, Month.APRIL, 1 ), 2 );
	private final Employee dave = new Employee( 4l, "Dave Lee", getDate( 2010, Month.JULY, 21 ), 9 );

	private final Project moon = new Project( 2018, "BU007", 1, "Moon", "Travel to the Moon" );
	private final Project mars = new Project( 2020, "BU007", 1, "Mars", "Life on Mars?" );
	private final Project sun = new Project( 2040, "BU007", 1, "Sun", "Ride into the Sun" );

	@Before
	public void before() {
		inTransaction( em -> {
			em.persist( john );
			em.persist( jane );
			em.persist( jake );
			em.persist( dave );
			em.persist( moon );
			em.persist( mars );
			em.persist( sun );
		} );
	}

	@After
	public void after() {
		inTransaction( em -> {
			List<Employee> allEmployee = em.createQuery( "from " + Employee.class.getName(), Employee.class ).getResultList();
			assertThat( allEmployee ).hasSize( 4 );
			for ( Employee employee : allEmployee ) {
				em.remove( employee );
			}

			List<Project> allProjects = em.createQuery( "from " + Project.class.getName(), Project.class ).getResultList();
			assertThat( allProjects ).hasSize( 3 );
			for ( Project project : allProjects ) {
				em.remove( project );
			}
		} );
	}

	@Test
	public void testSingleResultQuery() {
		inTransaction( em -> {
			Project result = (Project) em.createNativeQuery( "from HibernateOGMGenerated.Plan where title = 'Mars'", Project.class )
					.getSingleResult();

			assertThat( result ).isEqualTo( mars );
		} );
	}

	@Test
	public void testSingleResultQueryWithProjection() {
		inTransaction( em -> {
			// projection can be declared in native query text
			Object result = em.createNativeQuery( "select title from HibernateOGMGenerated.Plan where title = 'Mars'" )
					.getSingleResult();
			assertThat( result ).isEqualTo( mars.getName() );

			// projection can be declared as result set mapping
			result = em.createNativeQuery( "from HibernateOGMGenerated.Plan where title = 'Mars'", "titleMapping" )
					.getSingleResult();
			assertThat( result ).isEqualTo( mars.getName() );

			// or in both
			result = em.createNativeQuery( "select title from HibernateOGMGenerated.Plan where title = 'Mars'", "titleMapping" )
					.getSingleResult();
			assertThat( result ).isEqualTo( mars.getName() );
		} );
	}

	@Test
	public void testSingleResultQueryWithSeveralProjections() {
		String moonQuery = "from HibernateOGMGenerated.Plan where title = 'Moon' and description = 'Travel to the Moon'";

		inTransaction( em -> {
			Query nativeQuery = em.createNativeQuery( moonQuery, "multiValueMapping" );

			Object result = nativeQuery.getSingleResult();
			assertThat( result ).isEqualTo( new Object[] { moon.getName(), moon.getDescription(), moon.getFiscalYear() } );

			List<Object> results = nativeQuery.getResultList();
			assertThat( results.get( 0 ) ).isEqualTo( new Object[] { moon.getName(), moon.getDescription(), moon.getFiscalYear() } );
		} );
	}

	@Test
	public void testSingleResultFromNamedNativeQuery() {
		inTransaction( em -> {
			Employee result = (Employee) em.createNamedQuery( "JohnQuery" ).getSingleResult();

			assertThat( result ).isEqualTo( john );
		} );
	}

	@Test
	public void testSingleProjectionResultFromNamedNativeQuery() {
		inTransaction( em -> {
			Object result = em.createNamedQuery( "sunQuery" ).getSingleResult();

			assertThat( result ).isEqualTo( sun.getName() );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class, Project.class };
	}

	private Date getDate(int year, Month month, int day) {
		return Date.from( LocalDate.of( year, month, day ).atStartOfDay( ZoneId.systemDefault() ).toInstant() );
	}
}
