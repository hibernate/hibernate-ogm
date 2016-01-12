/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigInteger;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the execution of native queries on Cassandra using the {@link Session}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Jonathan Halliday
 */
public class CassandraSessionNativeQueryTest extends OgmTestCase {

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde" );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde", (byte) 2, new BigInteger( "10" ) );
	private final OscarWildePoem imperatrix = new OscarWildePoem( 3L, "Ave Imperatrix", "Oscar Wilde" );

	@Before
	public void init() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		session.persist( portia );
		session.persist( athanasia );
		session.persist( imperatrix );
		transaction.commit();
		session.clear();
		session.close();
	}

	@After
	public void tearDown() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		delete( session, portia );
		delete( session, athanasia );
		delete( session, imperatrix );
		tx.commit();
		session.clear();
		session.close();
	}

	private void delete(Session session, OscarWildePoem poem) {
		Object entity = session.get( OscarWildePoem.class, poem.getId() );
		if ( entity != null ) {
			session.delete( entity );
		}
	}

	@Test
	public void testNativeQueryWithFirstResult() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		Query query = session.createNativeQuery( "SELECT * FROM \"WILDE_POEM\"" )
				.addEntity( OscarWildePoem.class )
				.setFirstResult( 1 );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result ).hasSize( 2 );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testNativeQueryWithMaxRows() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		Query query = session.createNativeQuery( "SELECT * FROM \"WILDE_POEM\"" )
				.addEntity( OscarWildePoem.class )
				.setMaxResults( 2 );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result ).hasSize( 2 );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testListMultipleResultQuery() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "SELECT * FROM \"WILDE_POEM\" WHERE author='Oscar Wilde'";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.list();

		assertThat( result ).onProperty( "id" ).contains( 2L, 3L, 1L );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testListMultipleResultQueryWithFirstResultAndMaxRows() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "SELECT * FROM \"WILDE_POEM\" WHERE author='Oscar Wilde'";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.setFirstResult( 1 )
				.setMaxResults( 1 )
				.list();

		assertThat( result ).onProperty( "id" ).hasSize( 1 );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testUniqueResultNamedNativeQuery() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		try {
			OscarWildePoem uniqueResult = (OscarWildePoem) session.getNamedQuery( "AthanasiaQuery" )
					.uniqueResult();
			assertAreEquals( uniqueResult, athanasia );
			transaction.commit();
		}
		finally {
			session.clear();
			session.close();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testEntitiesInsertedInCurrentSessionAreFoundByNativeQuery() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "SELECT * FROM \"WILDE_POEM\" WHERE name='Her Voice'";

		Query query = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.class );

		List<OscarWildePoem> result = query.list();
		assertThat( result ).isEmpty();

		OscarWildePoem voice = new OscarWildePoem( 4L, "Her Voice", "Oscar Wilde" );
		session.persist( voice );

		result = query.list();
		assertThat( result ).onProperty( "id" ).containsExactly( 4L );

		transaction.commit();

		transaction = session.beginTransaction();
		session.delete( voice );
		transaction.commit();

		session.close();
	}

	@Test
	public void testUniqueResultFromNativeQueryWithParameter() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		try {
			String nativeQuery = "SELECT * FROM \"WILDE_POEM\" WHERE name=?";
			SQLQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			query.setString( 0, "Portia" );

			OscarWildePoem uniqueResult = (OscarWildePoem) query.uniqueResult();
			assertThat( uniqueResult ).isEqualTo( portia );
			transaction.commit();
		}
		finally {
			session.clear();
			session.close();
		}
	}

	@Test
	public void testUniqueResultFromNativeQueryWithParameterRequiringTypeConversion() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		try {
			String nativeQuery = "SELECT * FROM \"WILDE_POEM\" WHERE score=?";
			SQLQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			query.setParameter( 0, new BigInteger( "10" ) );

			OscarWildePoem uniqueResult = (OscarWildePoem) query.uniqueResult();
			assertThat( uniqueResult ).isEqualTo( athanasia );
			transaction.commit();
		}
		finally {
			session.clear();
			session.close();
		}
	}

	private void assertAreEquals(OscarWildePoem expectedPoem, OscarWildePoem poem) {
		assertThat( poem ).isNotNull();
		assertThat( poem.getId() ).as( "Wrong Id" ).isEqualTo( expectedPoem.getId() );
		assertThat( poem.getName() ).as( "Wrong Name" ).isEqualTo( expectedPoem.getName() );
		assertThat( poem.getAuthor() ).as( "Wrong Author" ).isEqualTo( expectedPoem.getAuthor() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {OscarWildePoem.class};
	}

}
