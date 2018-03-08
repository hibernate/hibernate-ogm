/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.hibernate.ogm.backendtck.queries.projection.Movie;
import org.hibernate.ogm.backendtck.queries.projection.NativeQueryProjectionBaseTest;

import org.junit.Test;

/**
 * Use of a projection in Neoj4 Embedded native query:
 * if a projection is used on root Entity then the result must be an Object array instead of an Entity.
 *
 * addEntity native query parameter is not allowed then projection is used on root Entity.
 *
 * @author Fabio Massimo Ercoli
 */
public class Neo4jProjectionTest extends NativeQueryProjectionBaseTest {

	@Override
	protected String getNativeQueryWithoutProjection() {
		return "MATCH ( p:Movie ) RETURN p";
	}

	@Override
	protected String getNativeQueryWithProjectionIdName() {
		return "MATCH ( p:Movie ) RETURN p.id, p.name";
	}

	@Override
	protected String getNativeQueryWithProjectionYearAuthor() {
		return "MATCH ( p:Movie ) RETURN p.year, p.author";
	}

	@Test
	public void testResultListWithoutAddEntityWithProjection() {
		inTransaction( session -> {
			List<Movie> movies = session.createNativeQuery( getNativeQueryWithProjectionYearAuthor() )
				.getResultList();

			assertThat( movies.get( 0 ) ).isEqualTo( new Object[] { 1968, "Stanley Kubrick" } );
		} );
	}

}
