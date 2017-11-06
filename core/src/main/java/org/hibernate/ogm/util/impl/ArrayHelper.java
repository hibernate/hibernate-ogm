/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;

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
		return (String[][]) coll.toArray( new String[coll.size()][] );
	}

	/**
	 * Create a smaller array from an existing one.
	 *
	 * @param strings an array containing element of type {@link String}
	 * @param begin the starting position of the sub-array
	 * @param length the number of element to consider
	 * @return a new array continaining only the selected elements
	 */
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
	 * @param <T> the type of elements in the array
	 * @return the position of the element if it's found in the array, -1 otherwise
	 */
	public static <T> int indexOf(T[] array, T element) {
		for ( int i = 0; i < array.length; i++ ) {
			if ( array[i].equals( element ) ) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Check if an array contains an element.
	 *
	 * @param array the array with all the elements
	 * @param element the element to find in the array
	 * @return {@code true} if the array contains the element
	 */
	public static boolean contains(Object[] array, Object element) {
		return indexOf( array, element ) != -1;
	}

	/**
	 * Concats two arrays.
	 *
	 * @param first the first array
	 * @param second the second array
	 * @param <T> the type of the element in the array
	 * @return a new array created adding the element in the second array after the first one
	 */
	public static <T> T[] concat(T[] first, T... second) {
		int firstLength = first.length;
		int secondLength = second.length;

		@SuppressWarnings("unchecked")
		T[] result = (T[]) Array.newInstance( first.getClass().getComponentType(), firstLength + secondLength );
		System.arraycopy( first, 0, result, 0, firstLength );
		System.arraycopy( second, 0, result, firstLength, secondLength );

		return result;
	}

	/**
	 * Given a list of arrays, it returns a single array obtained concatenating the ones in the list.
	 * <p>
	 * Example: given the list ([1, 2, 3], [4], [4, 5]), it returns the array [1, 2, 3, 4, 4, 5].
	 *
	 * @param arrays the list containing the arrays
	 * @return a single array obtained concatenating the values in the initial list
	 */
	public static <T> T[] concat(List<T[]> arrays) {
		int totSize = 0;
		for ( int i = 0; i < arrays.size(); i++ ) {
			totSize += arrays.get( i ).length;
		}
		@SuppressWarnings("unchecked")
		T[] joined = (T[]) Array.newInstance( arrays.get( 0 ).getClass().getComponentType(), totSize );
		int currentLength = 0;
		for ( int i = 0; i < arrays.size(); i++ ) {
			T[] entry = arrays.get( i );
			System.arraycopy( entry, 0, joined, currentLength, entry.length );
			currentLength += entry.length;
		}
		return joined;
	}

	/**
	 * Concats an element and an array.
	 *
	 * @param firstElement the first element
	 * @param array the array
	 * @param <T> the type of the element in the array
	 * @return a new array created adding the element in the second array after the first element
	 */
	public static <T> T[] concat(T firstElement, T... array) {
		@SuppressWarnings("unchecked")
		T[] result = (T[]) Array.newInstance( firstElement.getClass(), 1 + array.length );
		result[0] = firstElement;
		System.arraycopy( array, 0, result, 1, array.length );

		return result;
	}
}
