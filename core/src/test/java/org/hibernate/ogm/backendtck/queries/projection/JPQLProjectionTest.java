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
public class JPQLProjectionTest extends SinglePoemBaseTest {

	public static final String JPQL_QUERY_WITHOUT_PROJECTION = "SELECT p FROM Poem p";
	public static final String JPQL_QUERY_WITH_PROJECTION = "SELECT p.id, p.name FROM Poem p";

	@Test
	public void testUniqueResultWithoutProjection() {
		inTransaction( session -> {
			Poem poem = (Poem) session.createQuery( JPQL_QUERY_WITHOUT_PROJECTION )
				.uniqueResult();

			assertThat( poem ).isEqualTo( originalPoem );
		} );
	}

	@Test
	public void testUniqueResultWithProjection() {
		inTransaction( session -> {
			Object poem = session.createQuery( JPQL_QUERY_WITH_PROJECTION )
				.uniqueResult();

			assertThat( poem ).isEqualTo( new Object[] { 1l, "Portia" } );
		} );
	}

	@Test
	public void testResultListWithoutProjection() {
		inTransaction( session -> {
			List<Poem> poems = session.createQuery( JPQL_QUERY_WITHOUT_PROJECTION )
				.getResultList();

			assertThat( poems ).isEqualTo( Collections.singletonList( originalPoem ) );
		} );
	}

	@Test
	public void testResultListWithProjection() {
		inTransaction( session -> {
			List<Object> poems = session.createQuery( JPQL_QUERY_WITH_PROJECTION )
				.getResultList();

			assertThat( poems.get( 0 ) ).isEqualTo( new Object[] { 1l, "Portia" } );
		} );
	}

}
