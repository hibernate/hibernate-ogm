/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.fest.assertions.Fail;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the execution of native queries on MongoDB using the {@link Session}
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

	private void delete(final Session session, final OscarWildePoem poem) {
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
		Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result ).onProperty( "id" ).containsOnly( portia.getId(), imperatrix.getId(), athanasia.getId() );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testFindOneWithPair() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ 'author' : 'Oscar Wilde' })";
		Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result.size() ).isEqualTo( 1 );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testFindAndModify() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
				+ ".findAndModify({ 'query': {'_id': 1}, 'update': { '$set': { 'author': 'Oscar Wilder' } }, 'new': true })";
		Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result.size() ).isEqualTo( 1 );
		assertThat( result.get( 0 ).getAuthor() ).isEqualTo( "Oscar Wilder" );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testFindAndModifyNoMatch() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
				+ ".findAndModify({ 'query': {'_id': 11}, 'update': { '$set': { 'author': 'Oscar Wilder' } }, 'new': true })";
		Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
		@SuppressWarnings("unchecked")
		List<OscarWildePoem> result = query.list();

		assertThat( result.size() ).isEqualTo( 0 );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testFindAndModifyNoMatchUpsertThenRemoveThenFindOne() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
				+ ".findAndModify({ 'query': {'_id': { '$numberLong': '11' } }, 'update': { '$set': { 'author': 'Oscar Wilder', 'name': 'The one and wildest', 'rating': '1' } }, 'new': true, 'upsert': true })";
		Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
		List<OscarWildePoem> result = query.list();

		assertThat( result.size() ).isEqualTo( 1 );
		assertThat( result.get( 0 ).getId() ).isEqualTo( 11 );
		assertThat( result.get( 0 ).getAuthor() ).isEqualTo( "Oscar Wilder" );
		assertThat( result.get( 0 ).getName() ).isEqualTo( "The one and wildest" );

		// Need to remove here because subsequent tests assume the initial dataset.
		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".remove({ '_id': { '$numberLong': '11' } })";
		query = session.createNativeQuery( nativeQuery );
		int n = query.executeUpdate();

		assertThat( n ).isEqualTo( 1 );

		// And check that it is gone.
		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ '_id': { '$numberLong': '11' } })";
		query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
		result = query.list();

		assertThat( result.size() ).isEqualTo( 0 );

		transaction.commit();

		session.clear();
		session.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInsertThenRemove() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
				+ ".insert({ '_id': { '$numberLong': '11' }, 'author': 'Oscar Wilder', 'name': 'The one and wildest', 'rating': '1' } )";
		Query query = session.createNativeQuery( nativeQuery );
		int n = query.executeUpdate();
		assertThat( n ).isEqualTo( 1 );

		// Try again.
		try {
			n = query.executeUpdate();
			Fail.fail( "Unique key constraint violation exception expected." );
		}
		catch (Exception e) {
			/* Expected */
		}

		// Check that it was inserted.
		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne( { 'name': 'The one and wildest' } )";
		query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );

		List<OscarWildePoem> result = query.list();
		assertThat( result.size() ).isEqualTo( 1 );
		assertThat( result.get( 0 ).getId() ).isEqualTo( 11 );
		assertThat( result.get( 0 ).getAuthor() ).isEqualTo( "Oscar Wilder" );
		assertThat( result.get( 0 ).getName() ).isEqualTo( "The one and wildest" );

		// Need to remove here because subsequent tests assume the initial dataset.
		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".remove({ '_id': { '$numberLong': '11' } })";
		query = session.createNativeQuery( nativeQuery );
		n = query.executeUpdate();
		assertThat( n ).isEqualTo( 1 );

		// And check that it is gone.
		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ '_id': { '$numberLong': '11' } })";
		query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
		result = query.list();
		assertThat( result.size() ).isEqualTo( 0 );

		transaction.commit();

		session.clear();
		session.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInsertMultipleThenRemove() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
				+ ".insert( [ { '_id': { '$numberLong': '11' }, 'author': 'Oscar Wilder', 'name': 'The one and wildest', 'rating': '1' }, { '_id': { '$numberLong': '12' }, 'author': 'Friedrich Schiller', 'name': 'An die Freude', 'rating': '1' } ], { 'ordered': false } )";
		Query query = session.createNativeQuery( nativeQuery );
		int n = query.executeUpdate();
		assertThat( n ).isEqualTo( 2 );

		// Try again.
		try {
			n = query.executeUpdate();
			Fail.fail( "Unique key constraint violation exception expected." );
		}
		catch (Exception e) {
			/* Expected */
		}

		// Check that all were inserted.
		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne( { 'name': 'The one and wildest' } )";
		query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );

		List<OscarWildePoem> result = query.list();
		assertThat( result.size() ).isEqualTo( 1 );
		assertThat( result.get( 0 ).getId() ).isEqualTo( 11 );
		assertThat( result.get( 0 ).getAuthor() ).isEqualTo( "Oscar Wilder" );
		assertThat( result.get( 0 ).getName() ).isEqualTo( "The one and wildest" );

		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne( { 'name': 'An die Freude' } )";
		query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );

		result = query.list();
		assertThat( result.size() ).isEqualTo( 1 );
		assertThat( result.get( 0 ).getId() ).isEqualTo( 12 );
		assertThat( result.get( 0 ).getAuthor() ).isEqualTo( "Friedrich Schiller" );
		assertThat( result.get( 0 ).getName() ).isEqualTo( "An die Freude" );

		// Need to remove here because subsequent tests assume the initial dataset.
		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".remove({ '_id': { '$numberLong': '11' } })";
		query = session.createNativeQuery( nativeQuery );
		n = query.executeUpdate();
		assertThat( n ).isEqualTo( 1 );

		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".remove({ '_id': { '$numberLong': '12' } })";
		query = session.createNativeQuery( nativeQuery );
		n = query.executeUpdate();
		assertThat( n ).isEqualTo( 1 );

		// And check that they are gone.
		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ '_id': { '$numberLong': '11' } })";
		query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
		result = query.list();
		assertThat( result.size() ).isEqualTo( 0 );

		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ '_id': { '$numberLong': '12' } })";
		query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
		result = query.list();
		assertThat( result.size() ).isEqualTo( 0 );

		transaction.commit();

		session.clear();
		session.close();
	}

	@Test
	public void testFindWithAnd() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({ '$and': [{ 'author': 'Oscar Wilde' }, { 'name': 'Portia' }]})";
		Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
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
		Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
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
		Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
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
		Object result = session.createNativeQuery( nativeQuery ).uniqueResult();

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
			assertThat( message ).as( "The native query doesn't define a returned entity, there should be a specific exception" ).contains( "OGM001217" );
		}
		finally {
			session.clear();
			session.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1027")
	public void testNumberLongSupport() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".update({ '_id': 1}, { '$inc': { 'counter' : NumberLong(5) } })";
		Object result = session.createNativeQuery( nativeQuery ).executeUpdate();

		assertThat( result ).isEqualTo( 1 );

		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".update({ '_id': 1}, { '$inc': { 'counter' : new NumberLong(5) } })";
		result = session.createNativeQuery( nativeQuery ).executeUpdate();

		assertThat( result ).isEqualTo( 1 );

		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	@TestForIssue(jiraKey = "OGM-1027")
	public void testInsertMultipleWithNumberLongThenRemove() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
				+ ".insert( [ { '_id': NumberLong(11), 'author': 'Oscar Wilder', 'name': 'The one and wildest', 'rating': '1' }, { '_id': NumberLong(12), 'author': 'Friedrich Schiller', 'name': 'An die Freude', 'rating': '1' } ], { 'ordered': false } )";
		Query query = session.createNativeQuery( nativeQuery );
		int n = query.executeUpdate();
		assertThat( n ).isEqualTo( 2 );

		// Check that all were inserted.
		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne( { 'name': 'The one and wildest' } )";
		query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );

		List<OscarWildePoem> result = query.list();
		assertThat( result.size() ).isEqualTo( 1 );
		assertThat( result.get( 0 ).getId() ).isEqualTo( 11 );
		assertThat( result.get( 0 ).getAuthor() ).isEqualTo( "Oscar Wilder" );
		assertThat( result.get( 0 ).getName() ).isEqualTo( "The one and wildest" );

		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne( { 'name': 'An die Freude' } )";
		query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );

		result = query.list();
		assertThat( result.size() ).isEqualTo( 1 );
		assertThat( result.get( 0 ).getId() ).isEqualTo( 12 );
		assertThat( result.get( 0 ).getAuthor() ).isEqualTo( "Friedrich Schiller" );
		assertThat( result.get( 0 ).getName() ).isEqualTo( "An die Freude" );

		// Need to remove here because subsequent tests assume the initial dataset.
		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".remove({ '_id': NumberLong(11) })";
		query = session.createNativeQuery( nativeQuery );
		n = query.executeUpdate();
		assertThat( n ).isEqualTo( 1 );

		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".remove({ '_id': NumberLong(12) })";
		query = session.createNativeQuery( nativeQuery );
		n = query.executeUpdate();
		assertThat( n ).isEqualTo( 1 );

		// And check that they are gone.
		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ '_id': NumberLong(11) })";
		query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
		result = query.list();
		assertThat( result.size() ).isEqualTo( 0 );

		nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ '_id': NumberLong(12) })";
		query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
		result = query.list();
		assertThat( result.size() ).isEqualTo( 0 );

		transaction.commit();

		session.clear();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { OscarWildePoem.class };
	}
}
