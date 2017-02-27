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

import org.hibernate.ogm.datastore.impl.DatastoreProviderType;

/**
 * Allows to skip specific tests for certain datastore providers. If given on a test method and the containing test
 * class at the same time, the annotation declared on the method takes precedence.
 *
 * @author Gunnar Morling
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface SkipByDatastoreProvider {

	/**
	 * The datastore provider(s) on which to skip the test.
	 */
	DatastoreProviderType[] value();

	/**
	 * Optional comment describing the reason for skipping the test.
	 */
	String comment() default "";

}
