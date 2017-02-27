/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.shared;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.ogm.options.shared.impl.IndexOptionsConverter;
import org.hibernate.ogm.options.spi.MappingOption;
import org.hibernate.ogm.util.Experimental;

/**
 * Provide a way to specify options specific to a datastore.
 *
 * @author Guillaume Smet
 */
@Experimental
@Target( TYPE )
@Retention(RUNTIME)
@MappingOption(IndexOptionsConverter.class)
public @interface IndexOptions {

	/**
	 * Specifies the options for the indexes of this collection.
	 *
	 * @return the options for this collection.
	 */
	IndexOption[] value();

}
