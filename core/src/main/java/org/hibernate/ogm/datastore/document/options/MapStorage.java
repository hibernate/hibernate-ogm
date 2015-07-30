/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.options;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.ogm.datastore.document.options.impl.MapStorageConverter;
import org.hibernate.ogm.options.spi.MappingOption;

/**
 * Define the association storage type for the annotated entity or property. When given for a property which doesn't
 * represent an association, this setting is ignored.
 *
 * @author Gunnar Morling
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@MappingOption(MapStorageConverter.class)
public @interface MapStorage {

	/**
	 * The strategy for storing map-typed associations
	 *
	 * @return the strategy for storing map-typed associations
	 */
	MapStorageType value();
}
