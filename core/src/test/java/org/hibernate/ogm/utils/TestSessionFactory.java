/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Causes the session factory used by a test executed with {@link OgmTestRunner} to be injected into the annotated
 * field.
 *
 * @author Gunnar Morling
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TestSessionFactory {

	/**
	 * The scope of the session factory.
	 *
	 * @return the scope of the session factory
	 */
	Scope scope() default Scope.TEST_CLASS;

	public enum Scope {

		/**
		 * The same session factory instance is used for all test methods of the given test
		 */
		TEST_CLASS,

		/**
		 * A fresh session factory instance is used for each individual test methods of the given test
		 */
		TEST_METHOD;
	}
}
