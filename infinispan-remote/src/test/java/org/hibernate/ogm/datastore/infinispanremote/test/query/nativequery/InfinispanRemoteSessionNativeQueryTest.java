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

import org.hibernate.Session;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteServerRunner;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.query.NativeQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the execution of native queries on Infinispan Server using the {@link Session}
 *
 * @author Fabio Massimo Ercoli &lt;fabio@hibernate.org&gt;
 */
@RunWith(InfinispanRemoteServerRunner.class)
public class InfinispanRemoteSessionNativeQueryTest extends OgmTestCase {

	public static final String MORETHAN_LVL_3_ORDERBY_START = "from HibernateOGMGenerated.Registry where level > 3 order by start";

	private final Employee john = new Employee( 1l, "John Doe", getDate( 2017, Month.JUNE, 12 ), 4 );
	private final Employee jane = new Employee( 2l, "Jane Gee", getDate( 2016, Month.MARCH, 7 ), 7 );
	private final Employee jake = new Employee( 3l, "Jake Dee", getDate( 2011, Month.APRIL, 1 ), 2 );
	private final Employee dave = new Employee( 4l, "Dave Lee", getDate( 2010, Month.JULY, 21 ), 9 );

	@Before
	public void before() {
		inTransaction( session -> {
			session.persist( john );
			session.persist( jane );
			session.persist( jake );
			session.persist( dave );
		} );
	}

	@After
	public void after() {
		inTransaction( session -> {
			List<Employee> list = session.createQuery( "from " + Employee.class.getName(), Employee.class ).list();
			assertThat( list ).hasSize( 4 );
			for ( Employee employee : list ) {
				session.delete( employee );
			}
		} );
	}

	@Test
	public void testNativeQueryWithFirstResult() {
		inTransaction( session -> {
			List result = session.createNativeQuery( MORETHAN_LVL_3_ORDERBY_START )
					.addEntity( Employee.class )
					.setFirstResult( 1 )
					.list();

			assertThat( result ).containsExactly( jane, john );
		} );
	}

	@Test
	public void testNativeQueryWithMaxRows() {
		inTransaction( session -> {
			List result = session.createNativeQuery( MORETHAN_LVL_3_ORDERBY_START )
					.addEntity( Employee.class )
					.setMaxResults( 2 )
					.list();

			assertThat( result ).containsExactly( dave, jane );
		} );
	}

	@Test
	public void testListMultipleResultQuery() {
		inTransaction( session -> {
			List result = session.createNativeQuery( MORETHAN_LVL_3_ORDERBY_START )
					.addEntity( Employee.class )
					.list();

			assertThat( result ).containsExactly( dave, jane, john );
		} );
	}

	@Test
	public void testListMultipleResultQueryWithFirstResultAndMaxRows() {
		inTransaction( session -> {
			List result = session.createNativeQuery( MORETHAN_LVL_3_ORDERBY_START )
					.addEntity( Employee.class )
					.setFirstResult( 1 )
					.setMaxResults( 1 )
					.list();

			assertThat( result ).containsExactly( jane );
		} );
	}

	@Test
	public void testUniqueResultNamedNativeQuery() {
		inTransaction( session -> {
			Employee result = (Employee) session.createNamedQuery( "JohnQuery" )
					.getSingleResult();

			assertThat( result ).isEqualTo( john );
		} );
	}

	@Test
	public void testEntitiesInsertedInCurrentSessionAreFoundByNativeQuery() {
		Employee mike = new Employee( 5l, "Mike", getDate( 2020, Month.NOVEMBER, 21 ), 1 );

		inTransaction( session -> {
			String query = "from HibernateOGMGenerated.Registry where name = 'Mike'";

			NativeQuery nativeQuery = session.createNativeQuery( query )
					.addEntity( Employee.class );

			List result = nativeQuery.list();
			assertThat( result ).isEmpty();

			session.persist( mike );

			result = nativeQuery.list();
			assertThat( result ).containsExactly( mike );
		} );

		inTransaction( session -> {
			session.delete( mike );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Employee.class };
	}

	private Date getDate(int year, Month month, int day) {
		return Date.from( LocalDate.of( year, month, day ).atStartOfDay( ZoneId.systemDefault() ).toInstant() );
	}
}
