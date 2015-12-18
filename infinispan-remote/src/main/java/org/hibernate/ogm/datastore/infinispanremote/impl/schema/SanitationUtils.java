/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.schema;

public class SanitationUtils {

	public static String convertNameSafely(String name) {
		if ( name.indexOf( '.' ) != -1 ) {
			return name.replace( '.', '_' );//TODO verify against introducing ambiguities
		}
		else {
			return name;
		}
	}

	public static String toProtobufIdName(String name) {
		return name + "_id";
	}

	public static String qualify(final String name, final String protobufPackageName) {
		if ( protobufPackageName == null ) {
			return name;
		}
		else {
			return protobufPackageName + '.' + name;
		}
	}

}
