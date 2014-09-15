/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import java.lang.reflect.Array;
import java.util.Collection;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class ArrayHelper {
	public static final int[] EMPTY_INT_ARRAY = {};
	public static final String[] EMPTY_STRING_ARRAY = {};
	public static final Object[] EMPTY_OBJECT_ARRAY = {};

	public static String[] toStringArray(Collection coll) {
		return (String[]) coll.toArray( new String[coll.size()] );
	}

	public static String[][] to2DStringArray(Collection coll) {
		return (String[][]) coll.toArray( new String[ coll.size() ][] );
	}

	public static String[] slice(String[] strings, int begin, int length) {
		String[] result = new String[length];
		System.arraycopy( strings, begin, result, 0, length );
		return result;
	}

	/**
	 * Return the position of an element inside an array
	 *
	 * @param array the array where it looks for an element
	 * @param element the element to find in the array
	 * @return the position of the element if it's found in the array, -1 otherwise
	 */
	public static <T> int indexOf(T[] array, T element) {
		for ( int i = 0; i < array.length; i++ ) {
			if (array[i].equals( element )) {
				return i;
			}
		}
		return -1;
	}

	public static boolean contains(Object[] array, Object element) {
		return indexOf( array, element ) != -1;
	}

	public static <T> T[] concat(T[] first, T[] second) {
		int firstLength = first.length;
		int secondLength = second.length;

		@SuppressWarnings("unchecked")
		T[] result = (T[]) Array.newInstance( first.getClass().getComponentType(), firstLength + secondLength );
		System.arraycopy( first, 0, result, 0, firstLength );
		System.arraycopy( second, 0, result, firstLength, secondLength );

		return result;
	}
}
