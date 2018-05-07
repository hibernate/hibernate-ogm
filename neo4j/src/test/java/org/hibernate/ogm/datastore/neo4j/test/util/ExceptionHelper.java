/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.neo4j.test.util;

/**
 * @author The Viet Nguyen
 */
public class ExceptionHelper {

	private ExceptionHelper() {
		// util class
	}

	public static <T extends Throwable> T extract(Class<T> class1, Exception e) throws Throwable {
		Throwable cause = e;
		while ( cause != null ) {
			if ( cause.getClass().equals( class1 ) ) {
				break;
			}
			cause = cause.getCause();
		}
		if ( cause == null ) {
			throw e;
		}
		throw cause;
	}
}
