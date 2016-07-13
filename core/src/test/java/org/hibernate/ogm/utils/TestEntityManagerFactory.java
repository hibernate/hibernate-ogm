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

import org.hibernate.ogm.utils.jpa.OgmJpaTestRunner;

/**
 * Causes the entity manager factory used by a test executed with {@link OgmJpaTestRunner} to be injected into the annotated
 * field.
 *
 * @author Guillaume Smet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TestEntityManagerFactory {

	/**
	 * The scope of the entity manager factory.
	 *
	 * @return the scope of the entity manager factory
	 */
	Scope scope() default Scope.TEST_CLASS;

	public enum Scope {

		/**
		 * The same entity manager factory instance is used for all test methods of the given test
		 */
		TEST_CLASS,

		/**
		 * A fresh entity manager factory instance is used for each individual test methods of the given test
		 */
		TEST_METHOD;
	}
}
