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
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the pagination for native queries with MongoDB
 *
 * @author Fabio Massimo Ercoli
 */
public class MongoDBNativeQueryPaginationTest extends OgmTestCase {

	public static final String QUERY_FIND = "db.WILDE_POEM.find( { 'author' : 'Oscar Wilde' } )";
	public static final String QUERY_AGGREGATE_MATCH = "db.WILDE_POEM.aggregate( [ { '$match' : { 'author' : 'Oscar Wilde' } } ] )";

	private final OscarWildePoem portia = new OscarWildePoem( 1L, "Portia", "Oscar Wilde", 1881 );
	private final OscarWildePoem athanasia = new OscarWildePoem( 2L, "Athanasia", "Oscar Wilde", 1879 );
	private final OscarWildePoem imperatrix = new OscarWildePoem( 3L, "Ave Imperatrix", "Oscar Wilde", 1882 );
	private final OscarWildePoem intellectualis = new OscarWildePoem( 4L, "Amor Intellectualis", "Oscar Wilde", 1881 );
	private final OscarWildePoem apologia = new OscarWildePoem( 5L, "Apologias ", "Oscar Wilde", 1881 );
	private final OscarWildePoem easter = new OscarWildePoem( 6L, "Easter Day", "Oscar Wilde", 1881 );
	private final OscarWildePoem rome = new OscarWildePoem( 7L, "Rome Unvisited", "Oscar Wilde", 1881 );
	private final OscarWildePoem miniato = new OscarWildePoem( 8L, "San Miniato", "Oscar Wilde", 1881 );
	private final OscarWildePoem liberty = new OscarWildePoem( 9L, "Sonnet to Liberty", "Oscar Wilde", 1881 );
	private final OscarWildePoem vita = new OscarWildePoem( 10L, "Vita Nuova", "Oscar Wilde", 1881 );

	private final OscarWildePoem[] poems = { portia, athanasia, imperatrix, intellectualis, apologia, easter, rome, miniato, liberty, vita };

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

	private void delete(Session session, OscarWildePoem poem) {
		Object entity = session.get( OscarWildePoem.class, poem.getId() );
		if ( entity != null ) {
			session.delete( entity );
		}
	}

	@Test
	public void testFindPage1Size10() {
		inTransaction( session -> {
			List result = session.createNativeQuery( QUERY_FIND )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.setFirstResult( 0 )
				.setMaxResults( 10 )
				.list();

			assertThat( result ).containsOnly( poems );
		} );
	}

	@Test
	public void testAggregationPipelinePage1Size10() {
		inTransaction( session -> {
			List result = session.createNativeQuery( QUERY_AGGREGATE_MATCH )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.setFirstResult( 0 )
				.setMaxResults( 10 )
				.list();

			assertThat( result ).containsOnly( poems );
		} );
	}

	@Test
	public void testFindPage2Size5() {
		inTransaction( session -> {
			List result = session.createNativeQuery( QUERY_FIND )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.setFirstResult( 5 )
				.setMaxResults( 5 )
				.list();

			assertThat( result.size() ).isEqualTo( 5 );
		} );
	}

	@Test
	public void testAggregationMatchPage2Size5() {
		inTransaction( session -> {
			List result = session.createNativeQuery( QUERY_AGGREGATE_MATCH )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.setFirstResult( 5 )
				.setMaxResults( 5 )
				.list();

			assertThat( result.size() ).isEqualTo( 5 );
		} );
	}

	@Test
	public void testFindPage1Page2Size5() {
		inTransaction( session -> {
			List result = session.createNativeQuery( QUERY_FIND )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.setFirstResult( 0 )
				.setMaxResults( 5 )
				.list();

			assertThat( result.size() ).isEqualTo( 5 );

			result.addAll( session.createNativeQuery( QUERY_FIND )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.setFirstResult( 5 )
				.setMaxResults( 5 )
				.list() );

			assertThat( result ).containsOnly( poems );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "OGM-1411" )
	public void testAggregationMatchPage1Page2Size5() {
		inTransaction( session -> {
			List result = session.createNativeQuery( QUERY_AGGREGATE_MATCH )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.setFirstResult( 0 )
				.setMaxResults( 5 )
				.list();

			assertThat( result.size() ).isEqualTo( 5 );

			result.addAll(  session.createNativeQuery( QUERY_AGGREGATE_MATCH )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.setFirstResult( 5 )
				.setMaxResults( 5 )
				.list() );

			assertThat( result ).containsOnly( poems );
		} );
	}

	@Test
	public void testAggregationMatchPage1Size15() {
		inTransaction( session -> {
			List result = session.createNativeQuery( QUERY_AGGREGATE_MATCH )
				.addEntity( OscarWildePoem.TABLE_NAME, OscarWildePoem.class )
				.setFirstResult( 0 )
				.setMaxResults( 15 )
				.list();

			assertThat( result ).containsOnly( poems );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { OscarWildePoem.class };
	}

}
