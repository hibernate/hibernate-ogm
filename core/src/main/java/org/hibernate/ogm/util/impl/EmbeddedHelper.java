/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import java.util.regex.Pattern;

/**
 * @author Davide D'Alto
 */
public class EmbeddedHelper {

	private static final Pattern EMBEDDED_FIELDNAME_SEPARATOR = Pattern.compile( "\\." );

	public static String[] split(String columnName) {
		return EMBEDDED_FIELDNAME_SEPARATOR.split( columnName );
	}

	public static boolean isPartOfEmbedded(String columnName) {
		return columnName.contains( "." );
	}
}
