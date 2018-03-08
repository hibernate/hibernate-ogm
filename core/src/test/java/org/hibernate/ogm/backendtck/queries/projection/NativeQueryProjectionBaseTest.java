/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries.projection;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import javax.persistence.PersistenceException;

import org.hibernate.ogm.utils.TestForIssue;

import org.junit.Test;

/**
 * Base test for use of a projection:
 * if a projection is used on root Entity then the result must be an Object array instead of an Entity.
 *
 * addEntity native query parameter is not allowed then projection is used on root Entity.
 *
 * @author Fabio Massimo Ercoli
 */
public abstract class NativeQueryProjectionBaseTest extends SingleMovieBaseTest {

	protected abstract String getNativeQueryWithoutProjection();
	protected abstract String getNativeQueryWithProjectionIdName();
	protected abstract String getNativeQueryWithProjectionYearAuthor();

	@Test
	public void testUniqueResultWithAddEntityWithoutProjection() {
		inTransaction( session -> {
			Movie movie = (Movie) session.createNativeQuery( getNativeQueryWithoutProjection() )
				.addEntity( Movie.class )
				.uniqueResult();

			assertThat( movie ).isEqualTo( originalMovie );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "OGM-1375" )
	public void testUniqueResultWithAddEntityWithProjection() {
		thrown.expect( PersistenceException.class );
		thrown.expectMessage( PROJECTION_ADD_ENTITY_MESSAGE );

		inTransaction( session -> {
			session.createNativeQuery( getNativeQueryWithProjectionIdName() )
				.addEntity( Movie.class )
				.uniqueResult();
		} );
	}

	@Test
	public void testUniqueResultWithoutAddEntityWithProjection() {
		inTransaction( session -> {
			Object movie = session.createNativeQuery( getNativeQueryWithProjectionIdName() )
				.uniqueResult();

			assertThat( movie ).isEqualTo( new Object[] { 1l, "2001: A Space Odyssey" } );
		} );
	}

	@Test
	public void testResultListWithAddEntityWithoutProjection() {
		inTransaction( session -> {
			List<Movie> movies = session.createNativeQuery( getNativeQueryWithoutProjection() )
				.addEntity( Movie.class )
				.getResultList();

			assertThat( movies.get( 0 ) ).isEqualTo( originalMovie );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "OGM-1375" )
	public void testResultListWithAddEntityWithProjection() {
		thrown.expect( PersistenceException.class );
		thrown.expectMessage( PROJECTION_ADD_ENTITY_MESSAGE );

		inTransaction( session -> {
			session.createNativeQuery( getNativeQueryWithProjectionYearAuthor() )
				.addEntity( Movie.class )
				.getResultList();
		} );
	}

}
