/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.query.nativequery;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.infinispanremote.query.impl.InfinispanRemoteQueryDescriptor;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.InfinispanRemoteNativeQueryParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test to verify the behaviour of the target class {@link InfinispanRemoteNativeQueryParser}
 *
 * @author Davide D'Alto
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteNativeQueryParserTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testLoad() {
		String nativeQuery = "from org.hibernate.proto.Fruit";
		String expectedCacheName = "Fruit";

		assertParsingIsCorrect( nativeQuery, expectedCacheName );
	}

	@Test
	public void testLoadWithSpacesAndWeirdCase() {
		String nativeQuery = "             fROm              org.hibernate.proto.Fruit              ";
		String expectedCacheName = "Fruit";

		assertParsingIsCorrect( nativeQuery, expectedCacheName );
	}

	@Test
	public void testWhereWithSpacesAndWeirdCase() {
		String nativeQuery = "    fRom  proto.Flower                      WHERE           name=                            '         daisy' ";
		String expectedCacheName = "Flower";

		assertParsingIsCorrect( nativeQuery, expectedCacheName );
	}

	@Test
	public void testAlias() {
		String nativeQuery = "select a from org.hibernate.proto.Fruit a where a.color = 'red'";
		String expectedCacheName = "Fruit";

		assertParsingIsCorrect( nativeQuery, expectedCacheName );
	}

	@Test
	public void testUnusedAlias() {
		String nativeQuery = "from org.hibernate.proto.Fruit a";
		String expectedCacheName = "Fruit";

		assertParsingIsCorrect( nativeQuery, expectedCacheName );
	}

	@Test
	public void testUnusedAliasInProjections() {
		String nativeQuery = "SELECT color from org.hibernate.proto.Fruit a";
		String expectedCacheName = "Fruit";
		String[] expectedProjections = { "color" };

		assertParsingIsCorrect( nativeQuery, expectedCacheName, expectedProjections );
	}

	@Test
	public void testUnusedAliasWhereClause() {
		String nativeQuery = "from org.hibernate.proto.Fruit  a where    color='yellow'";
		String expectedCacheName = "Fruit";

		assertParsingIsCorrect( nativeQuery, expectedCacheName );
	}

	@Test
	public void testMixedAliasUsage() {
		String nativeQuery = "Select a.color  ,  name   from org.hibernate.proto.Fruit  a where    color='yellow' and a.name = 'apple'";
		String expectedCacheName = "Fruit";
		String[] expectedProjections = { "a.color", "name" };

		assertParsingIsCorrect( nativeQuery, expectedCacheName, expectedProjections );
	}

	@Test
	public void testAliasWithSpacesAndWeirdCase() {
		String nativeQuery = "              SELECT                   a                fROm org.hibernate.proto.Fruit               a            where a.color='red'";
		String expectedCacheName = "Fruit";

		assertParsingIsCorrect( nativeQuery, expectedCacheName );
	}

	@Test
	public void testProjection() {
		String nativeQuery = "select name, color from proto.Flower where name = 'daisy'";
		String expectedCacheName = "Flower";
		String[] expectedProjections = { "name", "color" };

		assertParsingIsCorrect( nativeQuery, expectedCacheName, expectedProjections );
	}

	@Test
	public void testProjectionWithSpacesAndWeirdCase() {
		String nativeQuery = "                     SELect          name              ,          color      from            proto.Flower      where name = 'daisy'";
		String expectedCacheName = "Flower";
		String[] expectedProjections = { "name", "color" };

		assertParsingIsCorrect( nativeQuery, expectedCacheName, expectedProjections );
	}

	@Test
	public void testProjectionAndAlias() {
		String nativeQuery = "select f.id, f.color from proto.Fruit f where a.color = 'red'";
		String expectedCacheName = "Fruit";
		String[] expectedProjections = { "f.id", "f.color" };

		assertParsingIsCorrect( nativeQuery, expectedCacheName, expectedProjections );
	}

	@Test
	public void testProjectionAndAliasWithSpacesAndWeirdCase() {
		String nativeQuery = "          SELECT          f.id           ,      f.color         FROM proto.Fruit         f        "
				+ "          where  a.color= 'red'                ";
		String expectedCacheName = "Fruit";
		String[] expectedProjections = { "f.id", "f.color" };

		assertParsingIsCorrect( nativeQuery, expectedCacheName, expectedProjections );
	}

	@Test
	public void testUpperCase() {
		String nativeQuery = "select a FROM org.hibernate.proto.Fruit a WHERE a.color = 'red'";
		String expectedCacheName = "Fruit";

		assertParsingIsCorrect( nativeQuery, expectedCacheName );
	}

	@Test
	public void testProjectionAndAliasUsingFrom() {
		String nativeQuery = "select f.ifrom , f.color from proto.Fruit f where a.color = 'red'";
		String expectedCacheName = "Fruit";
		String[] expectedProjections = { "f.ifrom", "f.color" };

		assertParsingIsCorrect( nativeQuery, expectedCacheName, expectedProjections );
	}

	@Test
	public void testProjectionAndAliasUsingFromWithSpacesAndWeirdCase() {
		String nativeQuery = "select                f.ifrom , f.color FROm                  proto.Fruit f WHEre   a.color = 'red'";
		String expectedCacheName = "Fruit";
		String[] expectedProjections = { "f.ifrom", "f.color" };

		assertParsingIsCorrect( nativeQuery, expectedCacheName, expectedProjections );
	}

	@Test
	public void testExceptionForQueriesWithMultipleCaches() {
		String nativeQuery = "FROm             proto.Fruit f, proto.Flower ";
		String expectedCacheName = "Fruit";

		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001721: Infinispan queries can only target a single entity type. Found [proto.Fruit f, proto.Flower] for query: " + nativeQuery );

		assertParsingIsCorrect( nativeQuery, expectedCacheName );
	}

	@Test
	public void testExceptionForQueriesWithoutFromClause() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001720: Missing from clause in native query" );

		String nativeQuery = "select proto.Fruit";
		String expectedCacheName = "Fruit";

		assertParsingIsCorrect( nativeQuery, expectedCacheName );
	}

	private void assertParsingIsCorrect(String nativeQuery, String cacheName) {
		assertParsingIsCorrect( nativeQuery, cacheName, null );
	}

	private void assertParsingIsCorrect(String nativeQuery, String cacheName, String[] projections) {
		InfinispanRemoteNativeQueryParser testSubject = new InfinispanRemoteNativeQueryParser( nativeQuery );
		InfinispanRemoteQueryDescriptor result = testSubject.parse();

		assertThat( result.getCache() ).isEqualTo( cacheName );
		assertThat( result.getQuery() ).isEqualTo( nativeQuery );
		assertThat( result.getProjections() ).isEqualTo( projections );
	}
}
