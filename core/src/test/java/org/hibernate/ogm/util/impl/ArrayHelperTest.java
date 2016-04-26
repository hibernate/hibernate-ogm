/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Sergey Chernolyas <sergey.chernolyas@gmail.com>
 */
public class ArrayHelperTest {

	/**
	 * Test of toStringArray method, of class ArrayHelper.
	 */
	@Test
	public void testToStringArray() {
		System.out.println( "toStringArray" );
		assertArrayEquals( new String[]{ "1", "2" }, ArrayHelper.toStringArray( Arrays.asList( new String[]{ "1", "2" } ) ) );
	}

	/**
	 * Test of to2DStringArray method, of class ArrayHelper.
	 */
	@Test
	public void testTo2DStringArray() {
		String[][] expectedArr = new String[2][];
		expectedArr[0] = new String[]{ "1" };
		expectedArr[1] = new String[]{ "2" };
		String[][] resultArr = ArrayHelper.to2DStringArray( Arrays.asList( new String[]{ "1", "2" } ) );
		for ( int i = 0; i < resultArr.length; i++ ) {
			String[] strings = resultArr[i];
			for ( int j = 0; j < strings.length; j++ ) {
				String string = strings[j];
				assertEquals( expectedArr[i][j], string );
			}
		}
	}

	/**
	 * Test of slice method, of class ArrayHelper.
	 */
	@Test
	public void testSlice() {
		System.out.println( "slice" );
		String[] strings = new String[]{ "1", "2", "3" };
		int begin = 0;
		int length = 2;
		String[] expResult = new String[]{ "1", "2" };
		String[] result = ArrayHelper.slice( strings, begin, length );
		assertArrayEquals( expResult, result );
	}

	/**
	 * Test of indexOf method, of class ArrayHelper.
	 */
	@Test
	public void testIndexOf() {
		System.out.println( "indexOf" );
		Object[] array = new Integer[]{ 1, 2, 3 };
		Object element = 3;
		int expResult = 2;
		int result = ArrayHelper.indexOf( array, element );
		assertEquals( expResult, result );
	}

	/**
	 * Test of contains method, of class ArrayHelper.
	 */
	@Test
	public void testContains() {
		System.out.println( "contains" );
		Object[] array = new Integer[]{ 1, 2, 3 };
		Object element = 3;
		boolean expResult = true;
		boolean result = ArrayHelper.contains( array, element );
		assertEquals( expResult, result );
	}

	/**
	 * Test of concat method, of class ArrayHelper.
	 */
	@Test
	public void testConcat() {
		System.out.println( "concat" );
		Object[] first = new String[]{ "1", "2" };
		Object[] second = new String[]{ "3" };
		Object[] expResult = new String[]{ "1", "2", "3" };
		Object[] result = ArrayHelper.concat( first, second );
		assertArrayEquals( expResult, result );
	}

}
