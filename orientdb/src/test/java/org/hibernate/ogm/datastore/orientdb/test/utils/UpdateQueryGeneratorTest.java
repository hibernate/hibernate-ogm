/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.test.utils;

import org.hibernate.ogm.datastore.orientdb.dto.GenerationResult;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hibernate.ogm.datastore.orientdb.utils.UpdateQueryGenerator;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class UpdateQueryGeneratorTest {

	public UpdateQueryGeneratorTest() {
	}

	@BeforeClass
	public static void setUpClass() {
	}

	@AfterClass
	public static void tearDownClass() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	/**
	 * Test of generate method, of class UpdateQueryGenerator.
	 */
	@Test
	public void testGenerate() {
		System.out.println( "generate" );
		String tableName = "tableName";
		Map<String, Object> valuesMap = new HashMap<>();
		valuesMap.put( "field1", 1 );
		valuesMap.put( "field2", "field2" );
		valuesMap.put( "field3.subfield1", 1 );
		valuesMap.put( "field3.subfield2", new byte[]{ 1, 2, 3 } );
		EntityKey primaryKey = new EntityKey( new DefaultEntityKeyMetadata( tableName, new String[]{ "id" } ), new Object[]{ 1 } );
		UpdateQueryGenerator instance = new UpdateQueryGenerator();
		GenerationResult result = instance.generate( tableName, valuesMap, primaryKey );
		System.out.println( "update query: " + result.getExecutionQuery() );
		Assert.assertThat( result.getExecutionQuery(), new BaseMatcher<String>() {

			@Override
			public boolean matches(Object o) {
				System.out.println( " o: " + o );
				String query = (String) o;
				return query.contains( "AQID" ) && query.contains( "field3.subfield2" );
			}

			@Override
			public void describeMismatch(Object o, Description d) {

			}

			@Override
			public void describeTo(Description d) {
				d.appendText( "containt binary data" );
			}
		} );

	}

}
