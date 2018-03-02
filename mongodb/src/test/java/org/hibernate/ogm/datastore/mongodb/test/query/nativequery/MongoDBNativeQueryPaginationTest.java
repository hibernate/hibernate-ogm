/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.nativequery;

import static java.util.Arrays.copyOfRange;
import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Test the pagination for native queries with MongoDB
 *
 * @author Davide D'Alto 
 * @author Fabio Massimo Ercoli
 */
@SuppressWarnings("unchecked")
public class MongoDBNativeQueryPaginationTest extends OgmTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	public static final String QUERY_FIND = "db.WILDE_POEM.find( { '$query' : { 'author' : 'Oscar Wilde' }, '$orderby' : { 'name' : 1 } } )";
	public static final String QUERY_AGGREGATE_MATCH = "db.WILDE_POEM.aggregate( [ { '$match' : { 'author' : 'Oscar Wilde' } }, { '$sort': {'name': 1 } } ] )";

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde", 1881 );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde", 1879 );
	private final OscarWildePoem imperatrix = new OscarWildePoem( 3L, "Ave Imperatrix", "Oscar Wilde", 1882 );
	private final OscarWildePoem intellectualis = new OscarWildePoem( 4L, "Amor Intellectualis", "Oscar Wilde", 1881 );
	private final OscarWildePoem apologia = new OscarWildePoem( 5L, "Apologias", "Oscar Wilde", 1881 );
	private final OscarWildePoem easter = new OscarWildePoem( 6L, "Easter Day", "Oscar Wilde", 1881 );
	private final OscarWildePoem rome = new OscarWildePoem( 7L, "Rome Unvisited", "Oscar Wilde", 1881 );
	private final OscarWildePoem miniato = new OscarWildePoem( 8L, "San Miniato", "Oscar Wilde", 1881 );
	private final OscarWildePoem liberty = new OscarWildePoem( 9L, "Sonnet to Liberty", "Oscar Wilde", 1881 );
	private final OscarWildePoem vita = new OscarWildePoem( 10L, "Vita Nuova", "Oscar Wilde", 1881 );

	private final OscarWildePoem[] poems = { intellectualis, apologia, athanasia, imperatrix, easter, portia, rome, miniato, liberty, vita };

	@Before
	public void init() {
		inTransaction( session -> {
			for ( OscarWildePoem poem : poems ) {
				session.persist( poem );
			}
		} );
	}

	@After
	public void tearDown() {
		inTransaction( session -> {
			for ( OscarWildePoem poem : poems ) {
				delete( session, poem );
			}
		} );
	}

	private List<?> runFindQuery(Session session, int startPosition, int maxResult) {
		List<OscarWildePoem> result = session.createNativeQuery( QUERY_FIND )
			.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
			.setFirstResult( startPosition )
			.setMaxResults( maxResult )
			.list();
		return result;
	}

	private List<?> runAggregateQuery(Session session, int startPosition, int maxResult) {
		List<OscarWildePoem> result = session.createNativeQuery( QUERY_FIND )
			.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
			.setFirstResult( startPosition )
			.setMaxResults( maxResult )
			.list();
		return result;
	}

	private void delete(Session session, OscarWildePoem poem) {
		Object entity = session.get( OscarWildePoem.class, poem.getId() );
		if ( entity != null ) {
			session.delete( entity );
		}
	}

	@Test
	public void testFirstPageWithFind() {
		inTransaction( session -> {
			List<?> result = runFindQuery( session, 0, 7 );
			assertThat( result ).containsExactly( copyOfRange( poems, 0, 7 ) );
		} );
	}

	@Test
	public void testFirstPageWithAggregate() {
		inTransaction( session -> {
			List<?> result = runAggregateQuery( session, 0, 10 );
			assertThat( result ).containsExactly( poems );
		} );
	}

	@Test
	public void testLastPageWithFind() {
		inTransaction( session -> {
			List<?> result = runFindQuery( session, 5, 5 );
			assertThat( result ).containsExactly( copyOfRange( poems, 5, 10 ) );
		} );
	}

	@Test
	public void testLastPageWithAggregate() {
		inTransaction( session -> {
			List<?> result = runAggregateQuery( session, 5, 5 );
			assertThat( result ).containsExactly( copyOfRange( poems, 5, 10 ) );
		} );
	}

	@Test
	public void testFirstTwoPagesWithFind() {
		inTransaction( session -> {
			List<?> result = runFindQuery( session, 0, 5 );
			assertThat( result ).containsExactly( copyOfRange( poems, 0, 5 ) );

			result = runFindQuery( session, 5, 5 );
			assertThat( result ).containsExactly( copyOfRange( poems, 5, 10 ) );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1411")
	public void testFirstTwoPagesWithAggregate() {
		inTransaction( session -> {
			List<?> result = runAggregateQuery( session, 0, 5 );
			assertThat( result ).containsExactly( copyOfRange( poems, 0, 5 ) );

			result = runAggregateQuery( session, 5, 5 );
			assertThat( result ).containsExactly( copyOfRange( poems, 5, 10 ) );
		} );
	}

	@Test
	public void testMaxResultsBiggerThanNumberOfResultsWithFind() {
		inTransaction( session -> {
			List<?> result = runFindQuery( session, 0, 15 );
			assertThat( result ).containsExactly( poems );
		} );
	}

	@Test
	public void testMaxResultsBiggerThanNumberOfResultsWithAggregate() {
		inTransaction( session -> {
			List<?> result = runAggregateQuery( session, 0, 15 );
			assertThat( result ).containsExactly( poems );
		} );
	}

	@Test
	public void testPageInTheMiddleWithFind() {
		inTransaction( session -> {
			List<?> result = runFindQuery( session, 3, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 3, 6 ) );
		} );
	}

	@Test
	public void testPageInTheMiddleWithAggregate() {
		inTransaction( session -> {
			List<?> result = runAggregateQuery( session, 3, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 3, 6 ) );
		} );
	}

	@Test
	public void testScrollingForwardWithFind() {
		inTransaction( session -> {
			List<?> result = runFindQuery( session, 3, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 3, 6 ) );

			result = runFindQuery( session, 6, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 6, 9 ) );
		} );
	}

	@Test
	public void testScrollingForwardWithAggregate() {
		inTransaction( session -> {
			List<?> result = runAggregateQuery( session, 3, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 3, 6 ) );

			result = runAggregateQuery( session, 6, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 6, 9 ) );
		} );
	}

	@Test
	public void testScrollingBackwardWithFind() {
		inTransaction( session -> {
			List<?> result = runFindQuery( session, 3, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 3, 6 ) );

			result = runFindQuery( session, 0, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 0, 3 ) );
		} );
	}

	@Test
	public void testScrollingBackwardWithAggregate() {
		inTransaction( session -> {
			List<?> result = runAggregateQuery( session, 3, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 3, 6 ) );

			result = runAggregateQuery( session, 0, 3 );
			assertThat( result ).containsExactly( copyOfRange( poems, 0, 3 ) );
		} );
	}

	@Test
	public void testStartPositionIsOutOfRangeWithFind() {
		inTransaction( session -> {
			List<?> result = runFindQuery( session, 10, 5 );
			assertThat( result ).isEmpty();
		} );
	}

	@Test
	public void testStartPositionIsOutOfRangeWithAggregate() {
		inTransaction( session -> {
			List<?> result = runAggregateQuery( session, 10, 5 );
			assertThat( result ).isEmpty();
		} );
	}

	@Test
	public void testStartPositionIsNegativeWithFind() {
		thrown.expect( IllegalArgumentException.class );

		inTransaction( session -> {
			runFindQuery( session, -4, 5 );
		} );
	}

	@Test
	public void testStartPositionIsNegativeWithAggregate() {
		thrown.expect( IllegalArgumentException.class );

		inTransaction( session -> {
			runAggregateQuery( session, -4, 5 );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { OscarWildePoem.class };
	}

}
