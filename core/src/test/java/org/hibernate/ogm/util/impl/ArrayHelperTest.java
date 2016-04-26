/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class ArrayHelperTest {

	@Test
	public void testToStringArray() {
		assertArrayEquals( new String[]{ "1", "2" }, ArrayHelper.toStringArray( Arrays.asList( new String[]{ "1", "2" } ) ) );
	}

	@Test
	public void testTo2DStringArray() {
		List<String[]> keyColumns = new ArrayList<>();
		keyColumns.add( new String[]{ "col1", "col2" } );
		keyColumns.add( new String[]{ "col3", "col4", "col5" } );
		keyColumns.add( new String[]{ "col1" } );

		String[][] expectedArr = new String[3][];
		expectedArr[0] = keyColumns.get( 0 );
		expectedArr[1] = keyColumns.get( 1 );
		expectedArr[2] = keyColumns.get( 2 );

		String[][] resultArr = ArrayHelper.to2DStringArray( keyColumns );
		assertThat( resultArr ).isEqualTo( expectedArr );
	}

	@Test
	public void testSlice() {
		String[] strings = new String[]{ "1", "2", "3" };
		int begin = 0;
		int length = 2;
		String[] expResult = new String[]{ "1", "2" };
		String[] result = ArrayHelper.slice( strings, begin, length );
		assertArrayEquals( expResult, result );
	}

	@Test
	public void testIndexOf() {
		Object[] array = new Integer[]{ 1, 2, 3 };
		Object element = 3;
		int expResult = 2;
		int result = ArrayHelper.indexOf( array, element );
		assertEquals( expResult, result );
	}

	@Test
	public void testContains() {
		Object[] array = new Integer[]{ 1, 2, 3 };
		Object element = 3;
		boolean expResult = true;
		boolean result = ArrayHelper.contains( array, element );
		assertEquals( expResult, result );
	}

	@Test
	public void testConcat() {
		Object[] first = new String[]{ "1", "2" };
		Object[] second = new String[]{ "3" };
		Object[] expResult = new String[]{ "1", "2", "3" };
		Object[] result = ArrayHelper.concat( first, second );
		assertArrayEquals( expResult, result );
	}
}
