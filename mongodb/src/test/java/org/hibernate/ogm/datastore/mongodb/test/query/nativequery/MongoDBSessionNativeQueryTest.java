/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.query.NativeQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *  Test the execution of native queries on MongoDB using the {@link Session}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class MongoDBSessionNativeQueryTest extends OgmTestCase {

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde", 1881 );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde", 1879 );
	private final OscarWildePoem imperatrix = new OscarWildePoem( 3L, "Ave Imperatrix", "Oscar Wilde", 1882 );

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

		NativeQuery query = session.createNativeQuery( "{ $query : { author : 'Oscar Wilde' }, $orderby : { name : 1 } }" )
				.addEntity( OscarWildePoem.class )
				.setFirstResult( 1 );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result ).onProperty( "id" ).containsExactly( 3L, 1L );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testNativeQueryWithMaxRows() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		NativeQuery query = session.createNativeQuery( "{ $query : { author : 'Oscar Wilde' }, $orderby : { name : 1 } }" )
				.addEntity( OscarWildePoem.class )
				.setMaxResults( 2 );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result ).onProperty( "id" ).containsExactly( 2L, 3L );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testListMultipleResultQuery() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "{ $query : { author : 'Oscar Wilde' }, $orderby : { name : 1 } }";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.list();

		assertThat( result ).onProperty( "id" ).containsExactly( 2L, 3L, 1L );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testListMultipleResultQueryWithFirstResultAndMaxRows() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "{ $query : { author : 'Oscar Wilde' }, $orderby : { name : 1 } }";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.setFirstResult( 1 )
				.setMaxResults( 1 )
				.list();

		assertThat( result ).onProperty( "id" ).containsExactly( 3L );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testExceptionWhenReturnedEntityIsMissingAndUniqueResultIsExpected() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "{ $and: [ { name : 'Portia' }, { author : 'Oscar Wilde' } ] }";
		try {
			session.createNativeQuery( nativeQuery ).uniqueResult();
			transaction.commit();
		}
		catch (Exception he) {
			transaction.rollback();
			String message = he.getMessage();
			assertThat( message )
				.as( "The native query doesn't define a returned entity, there should be a specific exception" )
				.contains( "OGM001217" );
		}
		finally {
			session.clear();
			session.close();
		}
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

		String nativeQuery = "{ name : 'Her Voice' }";

		NativeQuery query = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.class );

		List<OscarWildePoem> result = query.list();
		assertThat( result ).isEmpty();

		OscarWildePoem voice = new OscarWildePoem( 4L, "Her Voice", "Oscar Wilde", 1881 );
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
	public void testExceptionWhenReturnedEntityIsMissingAndManyResultsAreExpected() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "{ $query : { author : 'Oscar Wilde' }, $orderby : { name : 1 } }";
		try {
			session.createNativeQuery( nativeQuery ).list();
		}
		catch (Exception he) {
			transaction.rollback();
			String message = he.getMessage();
			assertThat( message )
				.as( "The native query doesn't define a returned entity, there should be a specific exception" )
				.contains( "OGM001217" );
		}
		finally {
			session.clear();
			session.close();
		}
	}

	@Test
	public void testQueryWithRegexOperator() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "{ $query : { author : { $regex : '^Oscar' } }, $orderby : { name : 1 } }";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.list();

		assertThat( result ).onProperty( "id" ).containsExactly( 2L, 3L, 1L );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testQueryWithOptions() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "{ $query : { author : 'Oscar Wilde' }, $orderby : { name : 1 }, $explain: false, $comment: 'a very useful comment',"
				+ "$maxTimeMS: 500 }";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.list();

		assertThat( result ).onProperty( "id" ).containsExactly( 2L, 3L, 1L );

		transaction.commit();
		session.clear();
		session.close();
	}

	private void assertAreEquals(OscarWildePoem expectedPoem, OscarWildePoem poem) {
		assertThat( poem ).isNotNull();
		assertThat( poem.getId() ).as( "Wrong Id" ).isEqualTo( expectedPoem.getId() );
		assertThat( poem.getName() ).as( "Wrong Name" ).isEqualTo( expectedPoem.getName() );
		assertThat( poem.getAuthor() ).as( "Wrong Author" ).isEqualTo( expectedPoem.getAuthor() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { OscarWildePoem.class };
	}

}
