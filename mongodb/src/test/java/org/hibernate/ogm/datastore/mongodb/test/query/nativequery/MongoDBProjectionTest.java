/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import javax.persistence.PersistenceException;

import org.hibernate.ogm.backendtck.queries.projection.Poem;
import org.hibernate.ogm.backendtck.queries.projection.SinglePoemBaseTest;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.Test;

/**
 * Use of a projection in MongoDB native query:
 * if a projection is used on root Entity then the result must be an Object array instead of an Entity.
 *
 * addEntity native query parameter is not allowed then projection is used on root Entity.
 *
 * @author Fabio Massimo Ercoli
 */
public class MongoDBProjectionTest extends SinglePoemBaseTest {

	public static final String NATIVE_QUERY_WITHOUT_PROJECTION = "db." + Poem.TABLE_NAME + ".find( {} )";
	public static final String NATIVE_QUERY_WITH_PROJECTION = "db." + Poem.TABLE_NAME + ".find( {}, { 'id' : 1, 'name' : 1  } )";

	@Test
	public void testUniqueResultWithAddEntityWithoutProjection() {
		inTransaction( session -> {
			Poem poem = (Poem) session.createNativeQuery( NATIVE_QUERY_WITHOUT_PROJECTION )
				.addEntity( Poem.class )
				.uniqueResult();

			assertThat( poem ).isEqualTo( originalPoem );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "OGM-1375" )
	public void testUniqueResultWithAddEntityWithProjection() {
		thrown.expect( PersistenceException.class );
		thrown.expectMessage( "OGM000090: addEntity is not allowed in native queries using projection on root Entity" );

		inTransaction( session -> {
			session.createNativeQuery( NATIVE_QUERY_WITH_PROJECTION )
				.addEntity( org.hibernate.ogm.backendtck.queries.projection.Poem.class )
				.uniqueResult();
		} );
	}

	@Test
	public void testUniqueResultWithoutAddEntityWithProjection() {
		inTransaction( session -> {
			Object poem = session.createNativeQuery( NATIVE_QUERY_WITH_PROJECTION )
				.uniqueResult();

			assertThat( poem ).isEqualTo( new Object[] { 1l, "Portia" } );
		} );
	}

	@Test
	public void testResultListWithAddEntityWithoutProjection() {
		inTransaction( session -> {
			List<Poem> poems = session.createNativeQuery( NATIVE_QUERY_WITHOUT_PROJECTION )
				.addEntity( Poem.class )
				.getResultList();

			assertThat( poems.get( 0 ) ).isEqualTo( originalPoem );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "OGM-1375" )
	public void testResultListWithAddEntityWithProjection() {
		thrown.expect( PersistenceException.class );
		thrown.expectMessage( "OGM000090: addEntity is not allowed in native queries using projection on root Entity" );

		inTransaction( session -> {
			session.createNativeQuery( NATIVE_QUERY_WITH_PROJECTION )
				.addEntity( org.hibernate.ogm.backendtck.queries.projection.Poem.class )
				.getResultList();
		} );
	}

	@Test
	public void testResultListWithoutAddEntityWithProjection() {
		inTransaction( session -> {
			List<Poem> poems = session.createNativeQuery( NATIVE_QUERY_WITH_PROJECTION )
				.getResultList();

			assertThat( poems.get( 0 ) ).isEqualTo( new Object[] { 1l, "Portia" } );
		} );
	}
}
