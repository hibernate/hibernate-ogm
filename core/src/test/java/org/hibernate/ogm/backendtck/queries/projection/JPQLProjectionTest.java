/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries.projection;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Use of projection in JPQL query:
 * if a projection is used on root Entity then the result must be an Object array instead of an Entity.
 *
 * @author Fabio Massimo Ercoli
 */
public class JPQLProjectionTest extends SingleMovieBaseTest {

	public static final String JPQL_QUERY_WITHOUT_PROJECTION = "SELECT p FROM Movie p";
	public static final String JPQL_QUERY_WITH_PROJECTION_ID_NAME = "SELECT p.id, p.name FROM Movie p";
	public static final String JPQL_QUERY_WITH_PROJECTION_YEAR_AUTHOR = "SELECT p.year, p.author FROM Movie p";

	@Test
	public void testUniqueResultWithoutProjection() {
		inTransaction( session -> {
			Movie movie = (Movie) session.createQuery( JPQL_QUERY_WITHOUT_PROJECTION )
				.uniqueResult();

			assertThat( movie ).isEqualTo( originalMovie );
		} );
	}

	@Test
	public void testUniqueResultWithProjection() {
		inTransaction( session -> {
			Object movie = session.createQuery( JPQL_QUERY_WITH_PROJECTION_ID_NAME )
				.uniqueResult();

			assertThat( movie ).isEqualTo( new Object[] { 1, "2001: A Space Odyssey" } );
		} );
	}

	@Test
	public void testResultListWithoutProjection() {
		inTransaction( session -> {
			List<Movie> movies = session.createQuery( JPQL_QUERY_WITHOUT_PROJECTION )
				.getResultList();

			assertThat( movies ).isEqualTo( Collections.singletonList( originalMovie ) );
		} );
	}

	@Test
	public void testResultListWithProjection() {
		inTransaction( session -> {
			List<Object> movies = session.createQuery( JPQL_QUERY_WITH_PROJECTION_YEAR_AUTHOR )
				.getResultList();

			assertThat( movies.get( 0 ) ).isEqualTo( new Object[] { 1968, "Stanley Kubrick" } );
		} );
	}

}
