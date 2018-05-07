/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.query.nativequery;

import static org.hibernate.ogm.utils.OgmAssertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;

import org.hibernate.ogm.datastore.infinispanremote.query.impl.InfinispanRemoteQueryDescriptor;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.InfinispanRemoteNativeQueryParser;

import org.junit.Test;

/**
 * Unit test to verify the behaviour of the target class {@link InfinispanRemoteNativeQueryParser}
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteNativeQueryParserTest {

	@Test
	public void testLoad() {
		String nativeQuery = "from org.hibernate.proto.Fruit";
		InfinispanRemoteNativeQueryParser testSubject = new InfinispanRemoteNativeQueryParser( nativeQuery );
		InfinispanRemoteQueryDescriptor result = testSubject.parse();
		assertThat( result ).isEqualTo( new InfinispanRemoteQueryDescriptor( "Fruit", nativeQuery, Collections.emptyList() ) );
	}

	@Test
	public void testWhere() {
		String nativeQuery = "from proto.Flower where name = 'daisy'";
		InfinispanRemoteNativeQueryParser testSubject = new InfinispanRemoteNativeQueryParser( nativeQuery );
		InfinispanRemoteQueryDescriptor result = testSubject.parse();
		assertThat( result ).isEqualTo( new InfinispanRemoteQueryDescriptor( "Flower", nativeQuery, Collections.emptyList() ) );
	}

	@Test
	public void testAlias() {
		String nativeQuery = "select a from org.hibernate.proto.Fruit a where a.color = 'red'";
		InfinispanRemoteNativeQueryParser testSubject = new InfinispanRemoteNativeQueryParser( nativeQuery );
		InfinispanRemoteQueryDescriptor result = testSubject.parse();
		assertThat( result ).isEqualTo( new InfinispanRemoteQueryDescriptor( "Fruit", nativeQuery, Collections.emptyList() ) );
	}

	@Test
	public void testProjection() {
		String nativeQuery = "select name, color from proto.Flower where name = 'daisy'";
		InfinispanRemoteNativeQueryParser testSubject = new InfinispanRemoteNativeQueryParser( nativeQuery );
		InfinispanRemoteQueryDescriptor result = testSubject.parse();

		ArrayList<String> expectedProjections = new ArrayList<>();
		expectedProjections.add( "name" );
		expectedProjections.add( "color" );

		assertThat( result ).isEqualTo( new InfinispanRemoteQueryDescriptor( "Flower", nativeQuery, expectedProjections ) );
	}

	@Test
	public void testProjectionAndAlias() {
		String nativeQuery = "select f.id, f.color from proto.Fruit f where a.color = 'red'";
		InfinispanRemoteNativeQueryParser testSubject = new InfinispanRemoteNativeQueryParser( nativeQuery );
		InfinispanRemoteQueryDescriptor result = testSubject.parse();

		ArrayList<String> expectedProjections = new ArrayList<>();
		expectedProjections.add( "id" );
		expectedProjections.add( "color" );

		assertThat( result ).isEqualTo( new InfinispanRemoteQueryDescriptor( "Fruit", nativeQuery, expectedProjections ) );
	}

	@Test
	public void testUpperCase() {
		String nativeQuery = "select a FROM org.hibernate.proto.Fruit a WHERE a.color = 'red'";
		InfinispanRemoteNativeQueryParser testSubject = new InfinispanRemoteNativeQueryParser( nativeQuery );
		InfinispanRemoteQueryDescriptor result = testSubject.parse();
		assertThat( result ).isEqualTo( new InfinispanRemoteQueryDescriptor( "Fruit", nativeQuery, Collections.emptyList() ) );
	}

	@Test
	public void testProjectionAndAliasUsingFrom() {
		String nativeQuery = "select f.ifrom , f.color from proto.Fruit f where a.color = 'red'";
		InfinispanRemoteNativeQueryParser testSubject = new InfinispanRemoteNativeQueryParser( nativeQuery );
		InfinispanRemoteQueryDescriptor result = testSubject.parse();

		ArrayList<String> expectedProjections = new ArrayList<>();
		expectedProjections.add( "ifrom" );
		expectedProjections.add( "color" );

		assertThat( result ).isEqualTo( new InfinispanRemoteQueryDescriptor( "Fruit", nativeQuery, expectedProjections ) );
	}
}
