/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.neo4j.test.query.nativequery.OscarWildePoem.TABLE_NAME;

import java.util.GregorianCalendar;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.impl.DatastoreProviderType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.query.NativeQuery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *  Test the execution of native queries on Neo4j using the {@link Session}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jSessionNativeQueryTest extends OgmTestCase {

	/*
	 *  The only purpose of this constant is to keep track of the nodes created using a native query via the
	 *	executeUpdate method. This is just a Label (not a table or entity).
	 */
	private static final String UPDATE_LABEL = "UPDATE";

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde", new GregorianCalendar( 1808, 3, 10, 12, 45 ).getTime() );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L	, "Athanasia", "Oscar Wilde", new GregorianCalendar( 1810, 3, 10 ).getTime() );
	private final OscarWildePoem ballade = new OscarWildePoem( 3L, "Ballade De Marguerite", "Oscar Wilde", new GregorianCalendar( 1881, 3, 1 ).getTime() );

	@Before
	public void init() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		session.persist( portia );
		session.persist( athanasia );
		session.persist( ballade );
		transaction.commit();
		session.clear();
		session.close();
	}

	@After
	public void deleteAll() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		session.createNativeQuery( "MATCH (n) DETACH DELETE n" ).executeUpdate();
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
	public void testUniqueResultQuery() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { name:'Portia', author:'Oscar Wilde' } ) RETURN n";
		OscarWildePoem poem = (OscarWildePoem) session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.uniqueResult();

		assertThat( poem ).isEqualTo( portia );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testListMultipleResultQuery() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " ) RETURN n ORDER BY n.name";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.list();

		assertThat( result ).as( "Unexpected number of results" ).containsExactly( athanasia, ballade, portia );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testListMultipleResultQueryWithoutReturnedType() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " ) RETURN n.name, n.author ORDER BY n.name";
		@SuppressWarnings("unchecked")
		List<Object[]> result = session.createNativeQuery( nativeQuery ).list();

		assertThat( result ).as( "Unexpected number of results" ).hasSize( 3 );

		Object[] athanasiaRow = result.get( 0 );
		assertThat( athanasiaRow[0] ).isEqualTo( athanasia.getName() );
		assertThat( athanasiaRow[1] ).isEqualTo( athanasia.getAuthor() );

		Object[] balladeRow = result.get( 1 );
		assertThat( balladeRow[0] ).isEqualTo( ballade.getName() );
		assertThat( balladeRow[1] ).isEqualTo( ballade.getAuthor() );

		Object[] portiaRow = result.get( 2 );
		assertThat( portiaRow[0] ).isEqualTo( portia.getName() );
		assertThat( portiaRow[1] ).isEqualTo( portia.getAuthor() );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testUniqueResultNamedNativeQuery() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		try {
			OscarWildePoem uniqueResult = (OscarWildePoem) session.getNamedQuery( "AthanasiaQuery" )
					.uniqueResult();
			assertThat( uniqueResult ).isEqualTo( athanasia );
			transaction.commit();
		}
		finally {
			session.clear();
			session.close();
		}
	}

	@Test
	public void testUniqueResultFromNativeQueryWithParameter() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		try {
			String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { name:{name}, author:'Oscar Wilde' } ) RETURN n";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			query.setParameter( "name", "Portia" );

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
	public void testNativeQueryWithFirstResult() throws Exception {
		OgmSession session = (OgmSession) openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { author:'Oscar Wilde' } ) RETURN n ORDER BY n.name";
		NativeQuery query = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.class )
				.setFirstResult( 1 );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result ).containsExactly( ballade, portia );

		transaction.commit();
		session.clear();
		session.close();

	}

	@Test
	public void testNativeQueryWithMaxRows() throws Exception {
		OgmSession session = (OgmSession) openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { author:'Oscar Wilde' } ) RETURN n ORDER BY n.name";
		NativeQuery query = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.class )
				.setMaxResults( 2 );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result ).containsExactly( athanasia, ballade );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testNativeQueryExecuteUpdate() throws Exception {
		OgmSession session = (OgmSession) openSession();
		Transaction transaction = session.beginTransaction();

		String findQueryString = "MATCH (n:" + UPDATE_LABEL + ") RETURN n";
		NativeQuery findQuery = session.createNativeQuery( findQueryString );

		String createQuery = "CREATE (n:" + UPDATE_LABEL + " { author:'Giorgio Faletti' })";
		int updates = session.createNativeQuery( createQuery ).executeUpdate();
		if ( TestHelper.getCurrentDatastoreProviderType() != DatastoreProviderType.NEO4J_HTTP ) {
			assertThat( updates ).isEqualTo( 3 ); // 1 node + 1 label + 1 property set
		}
		Object createdNode = findQuery.uniqueResult();
		assertThat( createdNode ).isNotNull();

		String deleteQuery = "MATCH (n:" + UPDATE_LABEL + ") DELETE n ";
		int deletes = session.createNativeQuery( deleteQuery ).executeUpdate();
		if ( TestHelper.getCurrentDatastoreProviderType() != DatastoreProviderType.NEO4J_HTTP ) {
			assertThat( deletes ).isEqualTo( 1 ); // 1 node
		}
		Object uniqueResult = findQuery.uniqueResult();
		assertThat( uniqueResult ).isNull();

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testNativeQueryExecuteUpdateValidation() throws Exception {
		Transaction transaction = null;
		try ( OgmSession session = (OgmSession) openSession() ) {
			transaction = session.beginTransaction();
			String createQuery = "CREATE (n:" + TABLE_NAME + " { id:'2387642528', author:'Giorgio Faletti' })";
			session.createNativeQuery( createQuery ).executeUpdate();
			session.createNativeQuery( createQuery ).executeUpdate();
			transaction.commit();
			Assert.fail( "Expected exception" );
		}
		catch (HibernateException he) {
			try {
				transaction.rollback();
			}
			catch (Exception e) {
				// Nothing to do
			}
			assertThat( he ).isInstanceOf( HibernateException.class );
			assertThat( he.getMessage() ).startsWith( "OGM001416" );
		}
	}

	@Test
	public void testListMultipleResultQueryWithFirstResultAndMaxRows() throws Exception {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "MATCH ( n:" + TABLE_NAME + " { author:'Oscar Wilde' } ) RETURN n ORDER BY n.name DESC";
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.setFirstResult( 1 )
				.setMaxResults( 1 )
				.list();

		assertThat( result ).containsExactly( ballade );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { OscarWildePoem.class, Critic.class };
	}

}
