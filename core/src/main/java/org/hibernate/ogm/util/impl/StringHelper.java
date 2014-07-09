/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class StringHelper {

	public static boolean isEmpty(String value) {
		return value != null ? value.length() == 0 : true;
	}

	public static boolean isNullOrEmptyString(Object value) {
		return value == null || value.toString().trim().isEmpty();
	}

	public static String toString(Object[] array) {
		int len = array.length;
		if ( len == 0 ) {
			return "";
		}
		StringBuilder buf = new StringBuilder( len * 12 );
		for ( int i = 0; i < len - 1; i++ ) {
			buf.append( array[i] ).append( ", " );
		}
		return buf.append( array[len - 1] ).toString();
	}

	/**
	 * Joins the elements of the given iterable to a string, separated by the given separator string.
	 *
	 * @param iterable the iterable to join
	 * @param separator the separator string
	 * @return a string made up of the string representations of the given iterable members, separated by the given
	 * separator string
	 */
	public static String join(Iterable<?> iterable, String separator) {
		if ( iterable == null ) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		boolean isFirst = true;

		for ( Object object : iterable ) {
			if ( !isFirst ) {
				sb.append( separator );
			}
			else {
				isFirst = false;
			}

			sb.append( object );
		}

		return sb.toString();
	}
}
