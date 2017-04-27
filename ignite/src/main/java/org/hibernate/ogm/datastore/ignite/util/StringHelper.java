/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.util;

/**
 * Some string transformation methods
 *
 * @author Victor Kadachigov
 */
public class StringHelper {
	public static String realColumnName(String fieldName) {
		return fieldName.replace( '.', '_' );
	}

	public static String stringBeforePoint(String value) {
		String result = value;
		int index = result.indexOf( '.' );
		if ( index >= 0 ) {
			result = result.substring( 0, index );
		}
		return result;
	}

	public static String stringAfterPoint(String value) {
		String result = value;
		int index = result.indexOf( '.' );
		if ( index >= 0 ) {
			result = result.substring( index + 1 );
		}
		return result;
	}
}
