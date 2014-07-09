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
 * Annotation to skip a specific test for certain grid dialects. If given on a test method and the containing test class
 * at the same time, the annotation declared on the method takes precedence.
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface SkipByGridDialect {

	/**
	 * The dialects against which to skip the test
	 *
	 * @return The dialects
	 */
	GridDialectType[] value();

	/**
	 * Comment describing the reason for the skip.
	 *
	 * @return The comment
	 */
	String comment() default "";

}
