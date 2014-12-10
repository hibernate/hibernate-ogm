/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.document.association.spi.impl;

import java.util.regex.Pattern;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class DocumentHelpers {

	private static final Pattern DOT_SEPARATOR_PATTERN = Pattern.compile( "\\." );

	/**
	 * If the column name is a dotted column, returns the first part.
	 * Returns null otherwise.
	 *
	 * @param column the column that might have a prefix
	 * @return the first part of the prefix of the column or {@code null} if the column does not have a prefix.
	 */
	public static String getPrefix(String column) {
		return column.contains( "." ) ? DOT_SEPARATOR_PATTERN.split( column )[0] : null;
	}

	/**
	 * Returns the shared prefix of these columns. Null otherwise.
	 *
	 * @param associationKeyColumns the columns sharing a prefix
	 * @return the shared prefix of these columns. {@code null} otherwise.
	 */
	public static String getColumnSharedPrefix(String[] associationKeyColumns) {
		String prefix = null;
		for ( String column : associationKeyColumns ) {
			String newPrefix = getPrefix( column );
			if ( prefix == null ) { // first iteration
				prefix = newPrefix;
				if ( prefix == null ) { // no prefix, quit
					break;
				}
			}
			else { // subsequent iterations
				if ( ! equals( prefix, newPrefix ) ) { // different prefixes
					prefix = null;
					break;
				}
			}
		}
		return prefix;
	}

	private static boolean equals(String left, String right) {
		return ( left == right ) || ( left != null && left.equals( right ) );
	}

}
