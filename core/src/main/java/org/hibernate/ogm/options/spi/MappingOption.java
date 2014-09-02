/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.spi;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Identify annotations that can be used as {@link Option}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
public @interface MappingOption {

	/**
	 * @return the converter class to use to convert the annotation into an {@link Option}
	 */
	Class<? extends AnnotationConverter<?>> value();

}
