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

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *  Test the execution of native queries on Neo4j using the {@link Session}
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jSessionNativeQueryTest extends OgmTestCase {

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde", new GregorianCalendar( 1808, 3, 10, 12, 45 ).getTime() );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde", new GregorianCalendar( 1810, 3, 10 ).getTime() );

	@Before
	public void init() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		session.persist( portia );
		session.persist( athanasia );
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

		assertAreEquals( portia, poem );

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

		assertThat( result ).as( "Unexpected number of results" ).hasSize( 2 );
		assertAreEquals( athanasia, result.get( 0 ) );
		assertAreEquals( portia, result.get( 1 ) );

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

		assertThat( result ).as( "Unexpected number of results" ).hasSize( 2 );

		Object[] athanasiaRow = result.get( 0 );
		assertThat( athanasiaRow[0] ).isEqualTo( athanasia.getName() );
		assertThat( athanasiaRow[1] ).isEqualTo( athanasia.getAuthor() );

		Object[] portiaRow = result.get( 1 );
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
			assertAreEquals( uniqueResult, athanasia );
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
			SQLQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			query.setParameter( "name", "Portia" );

			OscarWildePoem uniqueResult = (OscarWildePoem) query.uniqueResult();
			assertAreEquals( uniqueResult, portia );
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
		return new Class<?>[] { OscarWildePoem.class };
	}

}
