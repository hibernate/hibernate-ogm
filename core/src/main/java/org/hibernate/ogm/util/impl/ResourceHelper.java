/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public class ResourceHelper {

	private static final String FILE_DELIMITER = "\\A";

	public static String readResource(URL resource) throws IOException {
		try ( InputStream is = resource.openStream() ) {
			Scanner s = new Scanner( is ).useDelimiter( FILE_DELIMITER );
			return s.hasNext() ? s.next() : "";
		}
	}
}
