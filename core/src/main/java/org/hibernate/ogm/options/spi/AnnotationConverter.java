/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.spi;

import java.lang.annotation.Annotation;

/**
 * Convert option {@link Annotation}s into equivalent {@link OptionValuePair}s.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public interface AnnotationConverter<T extends Annotation> {

	/**
	 * Converts the given option annotation into an equivalent option value object.
	 *
	 * @param annotation the annotation to convert
	 * @return a option value representing the given option annotation
	 */
	OptionValuePair<?> convert(T annotation);

}
