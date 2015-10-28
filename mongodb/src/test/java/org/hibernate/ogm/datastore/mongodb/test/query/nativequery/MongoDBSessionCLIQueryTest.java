/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *  Test the execution of native queries on MongoDB using the {@link Session}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class MongoDBSessionCLIQueryTest extends OgmTestCase {

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde" );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde" );
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
	public void testFindWithPair() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({ 'author' : 'Oscar Wilde' })";
		Query query = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.class );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result ).onProperty( "id" ).containsOnly( portia.getId(), imperatrix.getId(), athanasia.getId() );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testFindWithAnd() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({ '$and': [{ 'author': 'Oscar Wilde' }, { 'name': 'Portia' }]})";
		Query query = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.class );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result ).onProperty( "id" ).containsOnly( portia.getId() );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testFindWithNor() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find( { '$nor' : [ { 'name' : 'Athanasia'}, { 'name' : 'Portia' }]})";
		Query query = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.class );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result ).onProperty( "id" ).containsOnly( imperatrix.getId() );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testFindWithNot() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find( { 'name' :  { '$not' : { '$eq' : 'Athanasia' }}})";
		Query query = session.createNativeQuery( nativeQuery )
				.addEntity( OscarWildePoem.class );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result ).onProperty( "id" ).containsOnly( portia.getId(), imperatrix.getId() );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testCountEntitiesQuery() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".count({ 'author' : 'Oscar Wilde' })";
		Object result = session.createNativeQuery( nativeQuery )
				.uniqueResult();

		assertThat( result ).isEqualTo( 3L );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testExceptionWhenReturnedEntityIsMissingAndUniqueResultIsExpected() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({ '$and': [ { 'name' : 'Portia' }, { 'author' : 'Oscar Wilde' } ] })";
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { OscarWildePoem.class };
	}
}
