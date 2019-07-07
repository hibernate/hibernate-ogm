/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.nativequery;

import org.hibernate.Session;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.isDataBaseExists;

/**
 * I created separate class for this feature, because
 * this test should start forever after all tests, otherwise
 * some tests can fall
 */

/**
 * @author Alexey Midinitsin
 */
public class MongoDBSessionCLIDropDBQueryTest extends OgmTestCase {

	private final MarkTwainPoem genius = new MarkTwainPoem( 1L, "Genius", "Mark Twain" );

	@Before
	public void init() {
		inTransaction( (session) -> {
			session.persist( genius );
		} );
	}

	@After
	public void tearDown() {
		inTransaction( (session) -> {
			delete( session, genius );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{MarkTwainPoem.class};
	}

	private void delete(final Session session, final MarkTwainPoem poem) {
		Object entity = session.get( MarkTwainPoem.class, poem.getId() );
		if ( entity != null ) {
			session.delete( entity );
		}
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1432")
	public void testForDropDatabase() {
		inTransaction( (session) -> {
			assertThat( isDataBaseExists( sessionFactory, MarkTwainPoem.TABLE_NAME ) ).isTrue();
			String nativeQuery = "db.dropDatabase()";
			int result = session.createNativeQuery( nativeQuery ).executeUpdate();
			assertThat( result ).isEqualTo( 1 );
			assertThat( !isDataBaseExists( sessionFactory, MarkTwainPoem.TABLE_NAME ) ).isTrue();
		} );
	}

}
