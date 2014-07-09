/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.ogm.datastore.mongodb.options.impl.ReadPreferenceConverter;
import org.hibernate.ogm.options.spi.MappingOption;

/**
 * Specifies the <a href="http://docs.mongodb.org/manual/core/read-preference/">read preference</a> to be applied when
 * performing read operations for the annotated entity or property.
 * <p>
 * When given on the property-level, this setting will only take effect when the property represents an association. If
 * given for non-association properties, the setting on the property-level will be ignored and the setting from the
 * entity will be applied.
 *
 * @author Gunnar Morling
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@MappingOption(ReadPreferenceConverter.class)
public @interface ReadPreference {

	/**
	 * Specifies the read concern setting to be applied when performing read operations for the annotated entity or
	 * property.
	 */
	ReadPreferenceType value();
}
