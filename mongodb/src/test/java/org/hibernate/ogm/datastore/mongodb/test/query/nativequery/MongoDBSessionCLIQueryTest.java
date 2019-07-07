/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.nativequery;

import com.mongodb.BasicDBList;
import org.fest.assertions.Fail;
import org.fest.assertions.MapAssert;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl.NativeQueryParseException;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.query.NativeQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.isCollectionExists;

/**
 * Test the execution of native queries on MongoDB using the {@link Session}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class MongoDBSessionCLIQueryTest extends OgmTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde", 1881, 15 );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde", 1879, 37, (byte) 5, "ebook" );
	private final OscarWildePoem imperatrix = new OscarWildePoem( 3L, "Ave Imperatrix", "Oscar Wilde", 1882, 48, (byte) 5, "audible", "ebook", "paperback" );
	private final MarkTwainPoem genius = new MarkTwainPoem( 1L, "Genius", "Mark Twain" );

	@Before
	public void init() {
		inTransaction( ( session ) -> {
			session.persist( portia );
			session.persist( athanasia );
			session.persist( imperatrix );
			session.persist( genius );
		} );
	}

	@After
	public void tearDown() throws InterruptedException {
		inTransaction( ( session ) -> {
			delete( session, portia );
			delete( session, athanasia );
			delete( session, imperatrix );
			delete( session, genius );
		} );
	}

	private void delete(final Session session, final MarkTwainPoem poem) {
		Object entity = session.get( MarkTwainPoem.class, poem.getId() );
		if ( entity != null ) {
			session.delete( entity );
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
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({ 'author' : 'Oscar Wilde' })";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsOnly( portia.getId(), imperatrix.getId(), athanasia.getId() );
		} );
	}

	@Test
	public void testFindOneWithPair() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ 'author' : 'Oscar Wilde' })";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).hasSize( 1 );
		} );
	}

	@Test
	public void testAggregate() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'author': 'Oscar Wilde' } }, { '$sort': {'name': -1 } } ])";

			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsExactly(
					portia.getId(),
					imperatrix.getId(),
					athanasia.getId()
			);
		} );
	}

	@Test
	public void testAggregateWithLessThan() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'year': { '$lt': 3333.3} } }, { '$sort': {'name': -1 } } ])";

			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsExactly(
					portia.getId(),
					imperatrix.getId(),
					athanasia.getId()
			);
		} );
	}

	@Test
	public void testAggregateGreaterThan() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'year': { '$gt': 0.3} } }, { '$sort': {'name': -1 } } ])";

			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsExactly(
					portia.getId(),
					imperatrix.getId(),
					athanasia.getId()
			);
		} );
	}

	@Test
	public void testFindAndModify() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".findAndModify({ 'query': {'_id': 1}, 'update': { '$set': { 'author': 'Oscar Wilder' } }, 'new': true })";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).getAuthor() ).isEqualTo( "Oscar Wilder" );
		} );
	}

	@Test
	public void testFindAndModifyNoMatch() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".findAndModify({ 'query': {'_id': 11}, 'update': { '$set': { 'author': 'Oscar Wilder' } }, 'new': true })";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).hasSize( 0 );
		} );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testFindAndModifyNoMatchUpsertThenRemoveThenFindOne() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".findAndModify({ 'query': {'_id': { '$numberLong': '11' } }, 'update': { '$set': { 'author': 'Oscar Wilder', 'name': 'The one and wildest', 'rating': '1' } }, 'new': true, 'upsert': true })";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			List<OscarWildePoem> result = query.list();

			assertThat( result ).hasSize( 1 );
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

			assertThat( result ).hasSize( 0 );
		} );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInsertThenRemove() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".insert({ '_id': { '$numberLong': '11' }, 'author': 'Oscar Wilder', 'name': 'The one and wildest', 'rating': '1' } )";
			NativeQuery query = session.createNativeQuery( nativeQuery );
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
			assertThat( result ).hasSize( 1 );
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
			assertThat( result ).hasSize( 0 );
		} );
	}


	@Test
	@TestForIssue(jiraKey = "OGM-1311")
	public void testInsertManyThenRemove() {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".insertMany( [ { '_id': { '$numberLong': '11' }, 'author': 'Oscar Wilder', 'name': 'The one and wildest', 'rating': '1' }, { '_id': { '$numberLong': '12' }, 'author': 'Friedrich Schiller', 'name': 'An die Freude', 'rating': '1' } ], { 'ordered': false } )";
			NativeQuery query = session.createNativeQuery( nativeQuery );
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
			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).getId() ).isEqualTo( 11 );
			assertThat( result.get( 0 ).getAuthor() ).isEqualTo( "Oscar Wilder" );
			assertThat( result.get( 0 ).getName() ).isEqualTo( "The one and wildest" );

			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne( { 'name': 'An die Freude' } )";
			query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );

			result = query.list();
			assertThat( result ).hasSize( 1 );
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
			assertThat( result ).hasSize( 0 );

			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ '_id': { '$numberLong': '12' } })";
			query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			result = query.list();
			assertThat( result ).hasSize( 0 );
		} );
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInsertMultipleThenRemove() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".insert( [ { '_id': { '$numberLong': '11' }, 'author': 'Oscar Wilder', 'name': 'The one and wildest', 'rating': '1' }, { '_id': { '$numberLong': '12' }, 'author': 'Friedrich Schiller', 'name': 'An die Freude', 'rating': '1' } ], { 'ordered': false } )";
			NativeQuery query = session.createNativeQuery( nativeQuery );
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
			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).getId() ).isEqualTo( 11 );
			assertThat( result.get( 0 ).getAuthor() ).isEqualTo( "Oscar Wilder" );
			assertThat( result.get( 0 ).getName() ).isEqualTo( "The one and wildest" );

			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne( { 'name': 'An die Freude' } )";
			query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );

			result = query.list();
			assertThat( result ).hasSize( 1 );
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
			assertThat( result ).hasSize( 0 );

			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ '_id': { '$numberLong': '12' } })";
			query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			result = query.list();
			assertThat( result ).hasSize( 0 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1310")
	public void testInsertOneThenRemove() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".insertOne({ '_id': { '$numberLong': '11' }, 'author': 'Oscar Wilder', 'name': 'The one and wildest', 'rating': '1' } )";
			NativeQuery query = session.createNativeQuery( nativeQuery );
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
			assertThat( result ).hasSize( 1 );
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
			assertThat( result ).hasSize( 0 );
		} );
	}

	@TestForIssue(jiraKey = "OGM-1315")
	public void testUpdateOne() throws Exception {
		inTransaction( ( session ) -> {
			String nativeUpdateOneQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".updateOne("
					+ "  { 'author' : 'Oscar Wilde' }, "
					+ "  { '$inc': { 'copiesSold' : 1 } } "
					+ ")";
			NativeQuery query = session.createNativeQuery( nativeUpdateOneQuery );

			int modifiedCount = query.executeUpdate();
			assertThat( modifiedCount ).isEqualTo( 1 );
			// Find updated one and check that it was updated.
			String nativeFindQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({ 'author' : 'Oscar Wilde' })";
			query = session.createNativeQuery( nativeFindQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();
			int modifiedElementIndex = 0;

			List<Integer> initialCopiesSoldList = Arrays.asList( portia.getCopiesSold(), athanasia.getCopiesSold(), imperatrix.getCopiesSold() );
			for ( int i = 0; i < result.size(); i++ ) {
				Integer currentCopiesSold = result.get( i ).getCopiesSold();
				if ( !initialCopiesSoldList.contains( currentCopiesSold ) ) {
					modifiedElementIndex = i;
					break;
				}
			}
			OscarWildePoem modifiedPoem = result.get( modifiedElementIndex );

			// Need to update back to original state because subsequent tests assume the initial dataset.
			session.clear();
			nativeUpdateOneQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".updateOne("
					+ "  { 'name' : '" + modifiedPoem.getName() + "' }, "
					+ "  { '$inc': { 'copiesSold' : -1 } } "
					+ ")";
			query = session.createNativeQuery( nativeUpdateOneQuery );

			modifiedCount = query.executeUpdate();
			assertThat( modifiedCount ).isEqualTo( 1 );

			// And check that it same as was.
			nativeFindQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({ 'name' : '" + modifiedPoem.getName() + "' })";
			query = session.createNativeQuery( nativeFindQuery ).addEntity( OscarWildePoem.class );
			result = query.list();

			assertThat( result ).hasSize( 1 );
			assertThat( initialCopiesSoldList ).contains( result.get( 0 ).getCopiesSold() );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "1316")
	public void testUpdateMany() throws Exception {
		inTransaction( ( session ) -> {
			String nativeUpdateManyQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".updateMany("
					+ "  { 'author' : 'Oscar Wilde' }, "
					+ "  { '$inc': { 'copiesSold' : 1 } } "
					+ ")";
			NativeQuery query = session.createNativeQuery( nativeUpdateManyQuery );
			int modifiedCount = query.executeUpdate();

			assertThat( modifiedCount ).isEqualTo( 3 );

			// Check that it was updated.
			String nativeFindQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({ 'author' : 'Oscar Wilde' })";
			query = session.createNativeQuery( nativeFindQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();
			List<Integer> initialRatingsList = Arrays.asList( portia.getCopiesSold(), athanasia.getCopiesSold(), imperatrix.getCopiesSold() );

			assertThat( result ).hasSize( initialRatingsList.size() );
			assertThat( result )
					.onProperty( "copiesSold" )
					.containsOnly( initialRatingsList.get( 0 ) + 1,
							initialRatingsList.get( 1 ) + 1,
							initialRatingsList.get( 2 ) + 1 );

			// Need to update back to original state because subsequent tests assume the initial dataset.
			session.clear();
			nativeUpdateManyQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".updateMany("
					+ "  { 'author' : 'Oscar Wilde' }, "
					+ "  { '$inc': { 'copiesSold' : -1 } }, "
					+ "  { 'upsert': true, 'writeConcern': {'w': 'majority', 'wtimeout' : 100 } } "
					+ ")";
			query = session.createNativeQuery( nativeUpdateManyQuery );

			modifiedCount = query.executeUpdate();
			assertThat( modifiedCount ).isEqualTo( 3 );

			// And check that it same as was.
			query = session.createNativeQuery( nativeFindQuery ).addEntity( OscarWildePoem.class );
			result = query.list();

			assertThat( result ).hasSize( 3 );
			assertThat( result )
					.onProperty( "copiesSold" )
					.containsOnly( initialRatingsList.toArray() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1313")
	public void testInsertThenDeleteOneWithOptions() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".insertOne({ '_id': { '$numberLong': '11' }, 'author': 'Oscar Wilder', 'name': 'The one and wildest', 'rating': '1' } )";
			NativeQuery query = session.createNativeQuery( nativeQuery );
			int n = query.executeUpdate();
			assertThat( n ).isEqualTo( 1 );

			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".deleteOne({ '_id': { '$numberLong': '11' } }, { 'w': 'majority', 'wtimeout' : 100 })";
			query = session.createNativeQuery( nativeQuery );
			n = query.executeUpdate();
			assertThat( n ).isEqualTo( 1 );

			// Check that it is gone.
			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ '_id': { '$numberLong': '11' } })";
			query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			List<OscarWildePoem> result = query.list();
			assertThat( result ).hasSize( 0 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1313")
	public void testInsertMultipleThenDeleteOne() throws Exception {
		inTransaction( ( session ) -> {

			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".insert( [ { '_id': { '$numberLong': '11' }, 'author': 'Oscar Wilde', 'name': 'Collection', 'rating': '1' }, { '_id': { '$numberLong': '12' }, 'author': 'Oscar Wilde', 'name': 'Collection', 'rating': '2' } ], { 'ordered': false } )";
			NativeQuery query = session.createNativeQuery( nativeQuery );
			int n = query.executeUpdate();
			assertThat( n ).isEqualTo( 2 );

			// Check that all were inserted.
			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".count({ 'name' : 'Collection' })";
			Object foundCount = session.createNativeQuery( nativeQuery ).uniqueResult();

			assertThat( foundCount ).isEqualTo( 2L );

			// Try to delete first
			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".deleteOne({ 'name': 'Collection' })";
			query = session.createNativeQuery( nativeQuery );
			n = query.executeUpdate();
			assertThat( n ).isEqualTo( 1 );

			// And check that it is gone.
			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ 'name': 'Collection' })";
			query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			List<OscarWildePoem> result = query.list();
			assertThat( result ).hasSize( 1 );

			// Try to delete second
			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".deleteOne({ 'name': 'Collection' })";
			query = session.createNativeQuery( nativeQuery );
			n = query.executeUpdate();
			assertThat( n ).isEqualTo( 1 );

			// And check that it is gone.
			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ 'name': 'Collection' })";
			query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			result = query.list();
			assertThat( result ).hasSize( 0 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1314")
	public void testInsertManyThenDeleteMany() {
		inTransaction( ( session ) -> {
			String nativeQueryForDeleteMany = "db." + OscarWildePoem.TABLE_NAME
					+ ".deleteMany( {'author': 'Oscar Wilde'} )";
			NativeQuery queryDeleteMany = session.createNativeQuery( nativeQueryForDeleteMany );
			int countOfDeleted = queryDeleteMany.executeUpdate();
			assertThat( countOfDeleted ).isEqualTo( 3 );

			String nativeQueryForFindDeletedEntity = "db." + OscarWildePoem.TABLE_NAME + ".find( {'author': 'Oscar Wilde'} )";
			NativeQuery queryFindDeletedEntity = session.createNativeQuery( nativeQueryForFindDeletedEntity ).addEntity( OscarWildePoem.class );
			List<OscarWildePoem> listOfFoundEntity = queryFindDeletedEntity.list();
			assertThat( listOfFoundEntity.size() ).isEqualTo( 0 );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1314")
	public void testInsertManyThenDeleteManyWithOptions() {
		inTransaction( ( session ) -> {
			String nativeQueryForInsertMany = "db." + OscarWildePoem.TABLE_NAME
					+ ".insert( "
					+ "[ { '_id': { '$numberLong': '11' }, 'author': 'Oscar Wilde', 'name': 'First name', 'rating': '8' }, "
					+ "{ '_id': { '$numberLong': '12' }, 'author': 'Oscar Wilde', 'name': 'Second name','rating': '8' }, "
					+ "{ '_id': { '$numberLong': '13' }, 'author': 'Oscar Wilde', 'name': 'Third name','rating': '8' } ], "
					+ "{ 'ordered': false } )";
			NativeQuery queryInsertMany = session.createNativeQuery( nativeQueryForInsertMany );
			int countOfInserted = queryInsertMany.executeUpdate();
			assertThat( countOfInserted ).isEqualTo( 3 );

			String nativeQueryForDeleteManyWithOptions = "db." + OscarWildePoem.TABLE_NAME
					+ ".deleteMany( {'rating': '8'}, { 'w': 'majority', 'wtimeout' : 100 } )";
			NativeQuery queryDeleteManyWithOptions = session.createNativeQuery( nativeQueryForDeleteManyWithOptions );
			int countOfDeleted = queryDeleteManyWithOptions.executeUpdate();
			assertThat( countOfDeleted ).isEqualTo( 3 );

			String nativeQueryForFindDeletedEntity = "db." + OscarWildePoem.TABLE_NAME + ".find( {'rating': '8'} )";
			NativeQuery queryFindDeletedEntity = session.createNativeQuery( nativeQueryForFindDeletedEntity ).addEntity( OscarWildePoem.class );
			List<OscarWildePoem> listOfFoundEntity = queryFindDeletedEntity.list();
			assertThat( listOfFoundEntity.size() ).isEqualTo( 0 );
		} );
	}

	@Test
	public void testFindWithAnd() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({ '$and': [{ 'author': 'Oscar Wilde' }, { 'name': 'Portia' }]})";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsOnly( portia.getId() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithMatchAndSort() {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'$or': [ {'author':'Oscar Wilde'}, {'name': 'Portia' }]}}, { '$sort' : { 'name' : -1 } }])";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsExactly(
					portia.getId(),
					imperatrix.getId(),
					athanasia.getId()
			);
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithMatchSortAndRegex() {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'$or': [ {'author': { '$regex': 'Oscar.*', '$options': 'i'}}, {'name': { '$regex': 'Po.*'} }]}}, { '$sort' : { 'name' : -1 } }])";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsExactly(
					portia.getId(),
					imperatrix.getId(),
					athanasia.getId()
			);
		} );
	}

	@Test
	public void testFindWithMax() {
		inTransaction( ( session ) -> {
			String queryJson = "'$query': { 'author': 'Oscar Wilde' } ";
			String max = " '$max': { 'year' : 1881 } ";
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({" + queryJson + "," + max + "})";

			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();
			assertThat( result ).onProperty( "id" ).containsExactly( athanasia.getId() );
		} );
	}

	@Test
	public void testFindWithMin() {
		inTransaction( ( session ) -> {
			String queryJson = "'$query': { 'author': 'Oscar Wilde' } ";
			String min = " '$min': { 'year' : 1882 } ";
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({" + queryJson + "," + min + "})";

			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();
			assertThat( result ).onProperty( "id" ).containsExactly( imperatrix.getId() );
		} );
	}

	@Test
	public void testFindWithModifiersWithEntity() {
		inTransaction( ( session ) -> {
			StringBuilder queryWithModifiers = new StringBuilder();
			queryWithModifiers.append( "'$query': { } " );
			queryWithModifiers.append( ", '$max': { 'year' : 1881 } " );
			queryWithModifiers.append( ", '$explain': false " );
			queryWithModifiers.append( ", '$snapshot': false " );
			queryWithModifiers.append( ", 'hint': { 'year' : 1881 } " );
			queryWithModifiers.append( ", 'maxScan': 11234" );

			queryWithModifiers.append( ", '$comment': 'Testing comment' " );
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({" + queryWithModifiers.toString() + "})";

			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();
			assertThat( result ).onProperty( "id" ).containsExactly( athanasia.getId() );
		} );
	}

	@Test
	public void testFindWithExplain() {
		inTransaction( ( session ) -> {
			StringBuilder queryWithModifiers = new StringBuilder();
			queryWithModifiers.append( "'$query': { 'author': 'Oscar Wilde' } " );
			queryWithModifiers.append( ", '$max': { 'year' : 1881 } " );
			queryWithModifiers.append( ", '$explain': true " );
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find({" + queryWithModifiers.toString() + "})";

			NativeQuery query = session.createNativeQuery( nativeQuery );
			@SuppressWarnings("unchecked")
			List<Object[]> result = query.list();
			// I'm not sure we can test the content because this is the result of the explain command
			// and I believe it might change among versions
			assertThat( result.get( 0 ) ).isNotEmpty();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithUnwindGroupAndSort() {
		inTransaction( ( session ) -> {
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

			NativeQuery query = session.createNativeQuery( nativeQuery );
			@SuppressWarnings("unchecked")
			List<Object[]> result = query.list();
			assertThat( result ).hasSize( 2 );

			BasicDBList expectedImperatrix = new BasicDBList();
			expectedImperatrix.addAll( imperatrix.getMediums() );
			assertThat( result.get( 0 ) ).isEqualTo( new Object[]{ imperatrix.getId(), expectedImperatrix } );

			BasicDBList expectedAthanasia = new BasicDBList();
			expectedAthanasia.addAll( athanasia.getMediums() );
			assertThat( result.get( 1 ) ).isEqualTo( new Object[] { athanasia.getId(), expectedAthanasia } );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithMatchSortAndRegexWithMaxResults() {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'$or': [ {'author': { '$regex': 'Oscar.*'}}, {'name': { '$regex': 'Po.*'} }]}}, { '$sort' : { 'name' : 1 } }])";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.setMaxResults( 2 ).list();

			assertThat( result ).onProperty( "id" ).containsOnly( athanasia.getId(), imperatrix.getId() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithMatchSortAndRegexWithFirstResult() {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'author': { '$regex': '.*'  } }}, { '$sort' : { 'name' : -1 } }])";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.setFirstResult( 1 ).list();

			assertThat( result ).onProperty( "id" ).containsExactly( imperatrix.getId(), athanasia.getId() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithMatchSortAndRegexWithFirstResultAndMaxResults() {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'$or': [ {'author': { '$regex': 'Oscar.*'}}, {'name': { '$regex': 'Po.*'} }]}}, { '$sort' : { 'name' : -1 } }])";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.setMaxResults( 1 ).setFirstResult( 1 ).list();

			assertThat( result ).onProperty( "id" ).containsExactly( imperatrix.getId() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1024")
	public void testAggregateWithMatchSortAndRegexWithOptions() {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".aggregate([{ '$match': {'$and': [ {'author': { '$regex': 'oscar.*', '$options': 'i' }}, {'name': { '$regex': 'po.*', '$options': 'i'} }]}}, { '$sort' : { 'name' : -1 } }])";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsExactly( portia.getId() );
		} );
	}

	@Test
	public void testFindWithNor() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find( { '$nor' : [ { 'name' : 'Athanasia'}, { 'name' : 'Portia' }]})";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsOnly( imperatrix.getId() );
		} );
	}

	@Test
	public void testFindWithNot() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".find( { 'name' :  { '$not' : { '$eq' : 'Athanasia' }}})";
			NativeQuery query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			@SuppressWarnings("unchecked")
			List<OscarWildePoem> result = query.list();

			assertThat( result ).onProperty( "id" ).containsOnly( portia.getId(), imperatrix.getId() );
		} );
	}

	@Test
	public void testCountEntitiesQuery() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".count({ 'author' : 'Oscar Wilde' })";
			Object result = session.createNativeQuery( nativeQuery ).uniqueResult();

			assertThat( result ).isEqualTo( 3L );
		} );
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
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".update({ '_id': 1}, { '$inc': { 'counter' : NumberLong(5) } })";
			Object result = session.createNativeQuery( nativeQuery ).executeUpdate();

			assertThat( result ).isEqualTo( 1 );

			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".update({ '_id': 1}, { '$inc': { 'counter' : new NumberLong(5) } })";
			result = session.createNativeQuery( nativeQuery ).executeUpdate();

			assertThat( result ).isEqualTo( 1 );
		} );
	}

	@Test
	@SuppressWarnings("unchecked")
	@TestForIssue(jiraKey = "OGM-1027")
	public void testInsertMultipleWithNumberLongThenRemove() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".insert( [ { '_id': NumberLong(11), 'author': 'Oscar Wilder', 'name': 'The one and wildest', 'rating': '1' }, { '_id': NumberLong(12), 'author': 'Friedrich Schiller', 'name': 'An die Freude', 'rating': '1' } ], { 'ordered': false } )";
			NativeQuery query = session.createNativeQuery( nativeQuery );
			int n = query.executeUpdate();
			assertThat( n ).isEqualTo( 2 );

			// Check that all were inserted.
			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne( { 'name': 'The one and wildest' } )";
			query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );

			List<OscarWildePoem> result = query.list();
			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).getId() ).isEqualTo( 11 );
			assertThat( result.get( 0 ).getAuthor() ).isEqualTo( "Oscar Wilder" );
			assertThat( result.get( 0 ).getName() ).isEqualTo( "The one and wildest" );

			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne( { 'name': 'An die Freude' } )";
			query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );

			result = query.list();
			assertThat( result ).hasSize( 1 );
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
			assertThat( result ).hasSize( 0 );

			nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".findOne({ '_id': NumberLong(12) })";
			query = session.createNativeQuery( nativeQuery ).addEntity( OscarWildePoem.class );
			result = query.list();
			assertThat( result ).hasSize( 0 );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { OscarWildePoem.class, MarkTwainPoem.class };
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1247")
	public void testDistinctQueryWithCriteriaAndCollation() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".distinct('name',{'author':'Oscar Wilde'},{'collation': { 'locale' : 'en', 'caseLevel' : false, 'caseFirst' : 'upper'}})";

			@SuppressWarnings("unchecked")
			List<String> result = (List<String>) session.createNativeQuery( nativeQuery ).uniqueResult();

			assertThat( result ).containsOnly( portia.getName(), imperatrix.getName(), athanasia.getName() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1247")
	public void testDistinctQueryWithoutCriteriaAndWIthCollation() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".distinct('name',{},{'collation': { 'locale' : 'en', 'caseLevel' : false, 'caseFirst' : 'upper'}})";

			@SuppressWarnings("unchecked")
			List<String> result = (List<String>) session.createNativeQuery( nativeQuery ).uniqueResult();

			assertThat( result ).containsOnly( portia.getName(), imperatrix.getName(), athanasia.getName() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1247")
	public void testDistinctQueryWithInCriteriaAndCollation() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME
					+ ".distinct('name', { '_id': {'$in' : [ " + portia.getId() + ", " + imperatrix.getId() + "]} }, {'collation': { 'locale' : 'en', 'caseLevel' : false, 'caseFirst' : 'upper'}})";

			@SuppressWarnings("unchecked")
			List<String> result = (List<String>) session.createNativeQuery( nativeQuery ).uniqueResult();

			assertThat( result ).containsOnly( portia.getName(), imperatrix.getName() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1247")
	public void testSimpleDistinctQuery() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".distinct('author')";

			@SuppressWarnings("unchecked")
			List<String> result = (List<String>) session.createNativeQuery( nativeQuery ).uniqueResult();

			assertThat( result ).containsOnly( "Oscar Wilde" );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1247")
	public void testDistinctQueryWithCriteria() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".distinct('name',{'author':'Oscar Wilde'})";

			@SuppressWarnings("unchecked")
			List<String> result = (List<String>) session.createNativeQuery( nativeQuery ).uniqueResult();

			assertThat( result ).containsOnly( portia.getName(), athanasia.getName(), imperatrix.getName() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1246")
	public void testSimpleMapReduce() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".mapReduce('function() { emit( this._id, this.copiesSold);}','function(keyId, values) { return Array.sum(values); }')";
			Map result = (Map) session.createNativeQuery( nativeQuery ).uniqueResult();
			assertThat( result ).hasSize( 3 );
			assertThat( result ).includes( MapAssert.entry( 1l, 15.0 ), MapAssert.entry( 2l, 37.0 ), MapAssert.entry( 3l, 48.0 ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1246")
	public void testMapReduceWithReplaceAction() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".mapReduce('function() { emit( this.author, this.copiesSold);}','function(keyId, values) { return Array.sum(values); }',{ 'out' : { 'replace' : 'WILDE_MAP_REDUCE' }  })";
			Map result = (Map) session.createNativeQuery( nativeQuery ).uniqueResult();
			assertThat( result ).hasSize( 1 );
			assertThat( result ).includes( MapAssert.entry( "Oscar Wilde", 100.0 ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1246")
	public void testMapReduceWithCollectionName() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".mapReduce('function() { emit( this.author, this.copiesSold);}','function(keyId, values) { return Array.sum(values); }',{ 'out' : 'WILDE_MAP_REDUCE'  })";
			Map result = (Map) session.createNativeQuery( nativeQuery ).uniqueResult();
			assertThat( result ).hasSize( 1 );
			assertThat( result ).includes( MapAssert.entry( "Oscar Wilde", 100.0 ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1246")
	public void testMapReduceWithQuery() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".mapReduce('function() { emit( this.author, this.copiesSold);}','function(keyId, values) { return Array.sum(values); }',{ 'out' : 'WILDE_MAP_REDUCE', 'query' : { 'year' : {'$gt' : 1881}}} )";
			Map result = (Map) session.createNativeQuery( nativeQuery ).uniqueResult();
			assertThat( result ).hasSize( 1 );
			assertThat( result ).includes( MapAssert.entry( "Oscar Wilde", 48.0 ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1246")
	public void testMapReduceWithOptions() throws Exception {
		inTransaction( ( session ) -> {
			String nativeQuery = "db." + OscarWildePoem.TABLE_NAME + ".mapReduce('function() { emit( this.author, this.copiesSold);}','function(keyId, values) { return Array.sum(values); }',{ 'out' : 'WILDE_MAP_REDUCE', 'query' : { 'year' : {'$gt' : 1880}}, 'limit' : 1, 'sort' : {'copiesSold' : 1 }, 'collation': { 'locale' : 'en', 'caseLevel' : false, 'caseFirst' : 'upper'} })";
			Map result = (Map) session.createNativeQuery( nativeQuery ).uniqueResult();
			assertThat( result ).hasSize( 1 );
			assertThat( result ).includes( MapAssert.entry( "Oscar Wilde", 15.0 ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1433")
	public void testDropCollection() {
		inTransaction( ( session ) -> {
			assertThat( isCollectionExists( sessionFactory, MarkTwainPoem.TABLE_NAME ) ).isTrue();

			String nativeQuery = "db." + MarkTwainPoem.TABLE_NAME + ".drop()";
			int result = session.createNativeQuery( nativeQuery ).executeUpdate();
			assertThat( result ).isEqualTo( 1 );
			assertThat( isCollectionExists( sessionFactory, MarkTwainPoem.TABLE_NAME ) ).isFalse();
			assertThat( isCollectionExists( sessionFactory, OscarWildePoem.TABLE_NAME ) ).isTrue();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1433")
	public void testDropCollectionThatDoesNotExist() {
		inTransaction( ( session ) -> {
			String fakeCollection = "IdoNotExistCollectionForTest";
			assertThat( isCollectionExists( sessionFactory, fakeCollection ) ).isFalse();

			String nativeQuery = "db." + fakeCollection + ".drop()";
			int result = session.createNativeQuery( nativeQuery ).executeUpdate();

			assertThat( result )
					.as( "Congratulations, you have solved an issue! So far we couldn't know if the collection was actually deleted so we always returned 1. If you fix this issue, update this test, please." )
					.isNotEqualTo( 0 );
			assertThat( isCollectionExists( sessionFactory, fakeCollection ) ).isFalse();
			assertThat( isCollectionExists( sessionFactory, OscarWildePoem.TABLE_NAME ) ).isTrue();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1433")
	public void testParametersAreInvalidWithDropCollectionOperation() {
		thrown.expect( NativeQueryParseException.class );
		thrown.expectMessage( "Parameters are invalid for drop" );

		inTransaction( ( session ) -> {
			String nativeQuery = "db." + MarkTwainPoem.TABLE_NAME + ".drop( 'parameter' )";
			session.createNativeQuery( nativeQuery ).executeUpdate();
		} );
	}

}
