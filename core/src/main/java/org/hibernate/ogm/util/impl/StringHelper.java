/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import java.util.Iterator;

/**
 * Utility functions for dealing with strings.
 *
 * @author Davide D'Alto
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class StringHelper {

	private static final String LINE_SEPARATOR;

	static {
		LINE_SEPARATOR = System.getProperty( "line.separator" );
	}

	public static boolean isEmpty(String value) {
		return value != null ? value.length() == 0 : true;
	}

	public static boolean isNullOrEmptyString(Object value) {
		return value == null || value.toString().trim().isEmpty();
	}

	// System#lineSeparator() is only available from Java 7 onwards
	public static String lineSeparator() {
		return LINE_SEPARATOR;
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
		return join( iterable.iterator(), separator );
	}

	public static String join(Iterator<?> iterator, String separator) {
		if ( iterator == null ) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		while ( iterator.hasNext() ) {
			sb.append( separator );
			sb.append( iterator.next() );
		}

		if ( sb.length() > 0 ) {
			return sb.substring( separator.length() );
		}
		return "";
	}

	/**
	 * If a text contains double quotes, escape them.
	 *
	 * @param text the text to escape
	 * @return Escaped text or {@code null} if the text is null
	 */
	public static String escapeDoubleQuotesForJson(String text) {
		if ( text == null ) {
			return null;
		}
		StringBuilder builder = new StringBuilder( text.length() );
		for ( int i = 0; i < text.length(); i++ ) {
			char c = text.charAt( i );
			switch ( c ) {
				case '"':
				case '\\':
					builder.append( "\\" );
				default:
					builder.append( c );
			}
		}
		return builder.toString();
	}
}
