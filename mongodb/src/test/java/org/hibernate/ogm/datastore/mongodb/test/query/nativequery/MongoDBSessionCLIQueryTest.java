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
import org.hibernate.ogm.datastore.impl.DatastoreProviderType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByDatastoreProvider;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBList;

/**
 * Test the execution of native queries on MongoDB using the {@link Session}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class MongoDBSessionCLIQueryTest extends OgmTestCase {

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde" );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde", "ebook" );
	private final OscarWildePoem imperatrix = new OscarWildePoem( 3L, "Ave Imperatrix", "Oscar Wilde", "audible", "ebook", "paperback" );

	@Before
	public void init() {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			session.persist( portia );
			session.persist( athanasia );
			session.persist( imperatrix );
			tx.commit();
		}
	}

	@After
	public void tearDown() throws InterruptedException {
		try ( Session session = openSession() ) {
			Transaction tx = session.beginTransaction();
			delete( session, portia );
			delete( session, athanasia );
			delete( session, imperatrix );
			tx.commit();
		}
	}

	private void delete(final Session session, final OscarWildePoem poem) {
		Object entity = session.get( OscarWildePoem.class, poem.getId() );
		if ( entity != null ) {
			session.delete( entity );
		}
	}

	@Test
	public void testFindWithPair() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({ 'author' : 'Oscar Wilde' })";
			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsOnly( portia.getId(), imperatrix.getId(), athanasia.getId() );

			transaction.commit();
		}
	}

	@Test
	public void testFindOneWithPair() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ 'author' : 'Oscar Wilde' })";
			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result.size() ).isEqualTo( 1 );

			transaction.commit();
		}
	}

	@Test
	public void testAggregate() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'author': 'Oscar Wilde' } }, { '$sort': {'name': -1 } } ])";

			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsExactly(
					portia.getId(),
					imperatrix.getId(),
					athanasia.getId()
			);

			transaction.commit();
		}
	}

	@Test
	public void testFindAndModify() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".findAndModify({ 'query': {'_id': 1}, 'update': { '$set': { 'author': 'Oscar Wilder' } }, 'new': true })";
			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result.size() ).isEqualTo( 1 );
			assertThat( result.get( 0 ).getAuthor() ).isEqualTo( "Oscar Wilder" );

			transaction.commit();
		}
	}

	@Test
	public void testFindAndModifyNoMatch() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".findAndModify({ 'query': {'_id': 11}, 'update': { '$set': { 'author': 'Oscar Wilder' } }, 'new': true })";
			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result.size() ).isEqualTo( 0 );

			transaction.commit();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testFindAndModifyNoMatchUpsertThenRemoveThenFindOne() throws Exception {
		try ( OgmSession session = openSession() ) {
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
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInsertThenRemove() throws Exception {
		try ( OgmSession session = openSession() ) {
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
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInsertMultipleThenRemove() throws Exception {
		try ( OgmSession session = openSession() ) {
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
		}
	}

	@Test
	public void testFindWithAnd() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({ '$and': [{ 'author': 'Oscar Wilde' }, { 'name': 'Portia' }]})";
			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsOnly( portia.getId() );

			transaction.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithMatchAndSort() {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'$or': [ {'author':'Oscar Wilde'}, {'name': 'Portia' }]}}, { '$sort' : { 'name' : -1 } }])";
			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsExactly(
					portia.getId(),
					imperatrix.getId(),
					athanasia.getId()
			);

			transaction.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithMatchSortAndRegex() {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'$or': [ {'author': { '$regex': 'Oscar.*', '$options': 'i'}}, {'name': { '$regex': 'Po.*'} }]}}, { '$sort' : { 'name' : -1 } }])";
			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsExactly(
					portia.getId(),
					imperatrix.getId(),
					athanasia.getId()
			);

			transaction.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithUnwindGroupAndSort() {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();
			String match = "{ '$match': { 'author':{'$regex':'o.*', '$options': 'i'}}}";
			String unwind = "{'$unwind': '$mediums'}";
			String group = "{ '$group': {'_id' : '$_id' ,'clicks' : {'$push':'$mediums'} } }";
			String sort = "{ '$sort': { '_id' : -1 } }";
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate(["
					+ match
					+ "," + unwind
					+ "," + group
					+ "," + sort
					+ "])";

			Query query = session.createNativeQuery( nativeQuery );
			@SuppressWarnings("unchecked")
			List<Object[]> result = query.list();
			assertThat( result ).hasSize( 2 );

			BasicDBList expectedImperatrix = new BasicDBList();
			expectedImperatrix.addAll( imperatrix.getMediums() );
			assertThat( result.get( 0 ) ).isEqualTo( new Object[]{ imperatrix.getId(), expectedImperatrix } );

			BasicDBList expectedAthanasia = new BasicDBList();
			expectedAthanasia.addAll( athanasia.getMediums() );
			assertThat( result.get( 1 ) ).isEqualTo( new Object[] { athanasia.getId(), expectedAthanasia } );

			transaction.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithMatchSortAndRegexWithMaxResults() {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'$or': [ {'author': { '$regex': 'Oscar.*'}}, {'name': { '$regex': 'Po.*'} }]}}, { '$sort' : { 'name' : 1 } }])";
			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.setMaxResults( 2 ).list();

			assertThat( result ).onProperty( "id" ).containsOnly( athanasia.getId(), imperatrix.getId() );

			transaction.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithMatchSortAndRegexWithFirstResult() {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'author': { '$regex': '.*'  } }}, { '$sort' : { 'name' : -1 } }])";
			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.setFirstResult( 1 ).list();

			assertThat( result ).onProperty( "id" ).containsExactly( imperatrix.getId(), athanasia.getId() );

			transaction.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithMatchSortAndRegexWithFirstResultAndMaxResults() {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'$or': [ {'author': { '$regex': 'Oscar.*'}}, {'name': { '$regex': 'Po.*'} }]}}, { '$sort' : { 'name' : -1 } }])";
			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.setMaxResults( 1 ).setFirstResult( 1 ).list();

			assertThat( result ).onProperty( "id" ).containsExactly( imperatrix.getId() );

			transaction.commit();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithMatchSortAndRegexWithOptions() {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'$and': [ {'author': { '$regex': 'oscar.*', '$options': 'i' }}, {'name': { '$regex': 'po.*', '$options': 'i'} }]}}, { '$sort' : { 'name' : -1 } }])";
			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsExactly( portia.getId() );

			transaction.commit();
			session.clear();
		}
	}

	@Test
	public void testFindWithNor() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find( { '$nor' : [ { 'name' : 'Athanasia'}, { 'name' : 'Portia' }]})";
			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsOnly( imperatrix.getId() );

			transaction.commit();
			session.clear();
		}
	}

	@Test
	public void testFindWithNot() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find( { 'name' :  { '$not' : { '$eq' : 'Athanasia' }}})";
			Query query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsOnly( portia.getId(), imperatrix.getId() );

			transaction.commit();
			session.clear();
		}
	}

	@Test
	public void testCountEntitiesQuery() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".count({ 'author' : 'Oscar Wilde' })";
			Object result = session.createNativeQuery( nativeQuery ).uniqueResult();

			assertThat( result ).isEqualTo( 3L );

			transaction.commit();
			session.clear();
		}
	}

	@Test
	public void testExceptionWhenReturnedEntityIsMissingAndUniqueResultIsExpected() throws Exception {
		try ( OgmSession session = openSession() ) {
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
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1027")
	public void testNumberLongSupport() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".update({ '_id': 1}, { '$inc': { 'counter' : NumberLong(5) } })";
			Object result = session.createNativeQuery( nativeQuery ).executeUpdate();

			assertThat( result ).isEqualTo( 1 );

			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".update({ '_id': 1}, { '$inc': { 'counter' : new NumberLong(5) } })";
			result = session.createNativeQuery( nativeQuery ).executeUpdate();

			assertThat( result ).isEqualTo( 1 );

			transaction.commit();
			session.clear();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	@TestForIssue(jiraKey = "OGM-1027")
	public void testInsertMultipleWithNumberLongThenRemove() throws Exception {
		try ( OgmSession session = openSession() ) {
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
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { OscarWildePoem.class };
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1247")
	@SkipByDatastoreProvider(value = DatastoreProviderType.FONGO, comment = "FongoDB does not support collation")
	public void testDistinctQueryWithCriteriaAndCollation() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".distinct('name',{'author':'Oscar Wilde'},{'collation': { 'locale' : 'en', 'caseLevel' : false, 'caseFirst' : 'upper'}})";

			@SuppressWarnings("unchecked")
			List<String> result = (List<String>) session.createNativeQuery( nativeQuery ).uniqueResult();

			assertThat( result ).containsOnly( portia.getName(), imperatrix.getName(), athanasia.getName() );

			transaction.commit();
			session.clear();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1247")
	@SkipByDatastoreProvider(value = DatastoreProviderType.FONGO, comment = "FongoDB does not support collation")
	public void testDistinctQueryWithoutCriteriaAndWIthCollation() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".distinct('name',{},{'collation': { 'locale' : 'en', 'caseLevel' : false, 'caseFirst' : 'upper'}})";

			@SuppressWarnings("unchecked")
			List<String> result = (List<String>) session.createNativeQuery( nativeQuery ).uniqueResult();

			assertThat( result ).containsOnly( portia.getName(), imperatrix.getName(), athanasia.getName() );

			transaction.commit();
			session.clear();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1247")
	@SkipByDatastoreProvider(value = DatastoreProviderType.FONGO, comment = "FongoDB does not support collation")
	public void testDistinctQueryWithInCriteriaAndCollation() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".distinct('name', { '_id': {'$in' : [ " + portia.getId() + ", " + imperatrix.getId() + "]} }, {'collation': { 'locale' : 'en', 'caseLevel' : false, 'caseFirst' : 'upper'}})";

			@SuppressWarnings("unchecked")
			List<String> result = (List<String>) session.createNativeQuery( nativeQuery ).uniqueResult();

			assertThat( result ).containsOnly( portia.getName(), imperatrix.getName() );

			transaction.commit();
			session.clear();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1247")
	@SkipByDatastoreProvider(value = DatastoreProviderType.FONGO, comment = "FongoDB does not support collation")
	public void testSimpleDistinctQuery() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".distinct('author')";

			@SuppressWarnings("unchecked")
			List<String> result = (List<String>) session.createNativeQuery( nativeQuery ).uniqueResult();

			assertThat( result ).containsOnly( "Oscar Wilde" );

			transaction.commit();
			session.clear();
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1247")
	@SkipByDatastoreProvider(value = DatastoreProviderType.FONGO, comment = "FongoDB does not support collation")
	public void testDistinctQueryWithCriteria() throws Exception {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".distinct('name',{'author':'Oscar Wilde'})";

			@SuppressWarnings("unchecked")
			List<String> result = (List<String>) session.createNativeQuery( nativeQuery ).uniqueResult();

			assertThat( result ).containsOnly( portia.getName(), athanasia.getName(), imperatrix.getName() );

			transaction.commit();
			session.clear();
		}
	}
}
