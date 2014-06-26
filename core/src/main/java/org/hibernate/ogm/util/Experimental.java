/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks the annotated API member as experimental, i.e. users or implementors of such API member should be prepared for
 * incompatible changes to this element in future revisions.
 * <p>
 * Examples of possible changes to experimental API members include:
 * <ul>
 * <li>signatures of experimental methods may change</li>
 * <li>experimental methods may be removed</li>
 * <li>new methods may be added to or removed from experimental interfaces which are intended to be implemented by
 * clients or integrators</li>
 * </ul>
 *
 * @author Gunnar Morling
 */
@Documented
@Retention(RetentionPolicy.CLASS)
public @interface Experimental {

	/**
	 * An optional description of the reasons for the annotated element to be marked as experimental.
	 */
	String value() default "";
}
