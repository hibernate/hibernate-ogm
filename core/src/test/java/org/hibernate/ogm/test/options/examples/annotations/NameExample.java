/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.examples.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.MappingOption;
import org.hibernate.ogm.options.spi.OptionValuePair;
import org.hibernate.ogm.test.options.examples.NameExampleOption;

/**
 * Annotation equivalent of {@link NameExampleOption}.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
@Target({ TYPE, FIELD, METHOD })
@Retention(RUNTIME)
@MappingOption(NameExample.NameExampleOptionConverter.class)
public @interface NameExample {

	String value();

	static class NameExampleOptionConverter implements AnnotationConverter<NameExample> {

		@Override
		public OptionValuePair<?> convert(NameExample annotation) {
			return OptionValuePair.getInstance( new NameExampleOption(), annotation.value() );
		}
	}
}
