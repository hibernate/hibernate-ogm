/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

/**
 * Base class of runtime exceptions raised by {@link GridDialect} implementations and handled by the Hibernate OGM
 * engine in a uniform fashion.
 *
 * @author Gunnar Morling
 */
public class GridDialectException extends RuntimeException {

	public GridDialectException(String message, Throwable cause) {
		super( message, cause );
	}

	public GridDialectException(String message) {
		super( message );
	}

	public GridDialectException(Throwable cause) {
		super( cause );
	}
}
