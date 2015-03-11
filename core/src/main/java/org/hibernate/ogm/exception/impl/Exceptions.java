/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.impl;

/**
 * Utility functions for dealing with exceptions.
 *
 * @author Gunnar Morling
 */
public class Exceptions {

	private Exceptions() {
	}

	/**
	 * Throws the given exception. Allows to re-throw checked exceptions also if they are not declared, exploiting type
	 * erasure by the compiler.
	 */
	@SuppressWarnings("unchecked")
	public static <E extends Exception> void sneakyThrow(Exception e) throws E {
		throw (E) e;
	}
}
