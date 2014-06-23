/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import org.hibernate.annotations.common.AssertionFailure;

/**
 * Utility for simple consistency checks of objects and parameters.
 *
 * @author Gunnar Morling
 */
public final class Contracts {

	private static final Log log = LoggerFactory.make();

	private Contracts() {
	}

	/**
	 * Asserts that the given object is not {@code null}.
	 *
	 * @param object the object to validate, e.g. a local variable etc.
	 * @param name the name of the object, will be used in the logging message in case the given object is {@code null}
	 * @throws IllegalArgumentException in case the given object is {@code null}
	 */
	public static void assertNotNull(Object object, String name) {
		if ( object == null ) {
			throw log.mustNotBeNull( name );
		}
	}

	/**
	 * Asserts that the given method or constructor is not {@code null}.
	 *
	 * @param parameter the parameter to validate
	 * @param parameterName the name of the parameter, will be used in the logging message in case the given object is
	 * {@code null}
	 * @throws IllegalArgumentException in case the given parameter is {@code null}
	 */
	public static void assertParameterNotNull(Object parameter, String parameterName) {
		if ( parameter == null ) {
			throw log.parameterMustNotBeNull( parameterName );
		}
	}

	public static void assertTrue(boolean condition, String message) {
		if ( !condition ) {
			throw new AssertionFailure( message );
		}
	}
}
