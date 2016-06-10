/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.shared;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.ogm.util.Experimental;

/**
 * Options specific to the datastore for a given index.
 *
 * @author Guillaume Smet
 */
@Experimental
@Target({})
@Retention(RUNTIME)
public @interface IndexOption {

	/**
	 * The name of the target index
	 *
	 * @return the name of the index
	 */
	String forIndex();

	/**
	 * A string containing the options (might be a JSON string for instance)
	 *
	 * @return the options
	 */
	String options();

}
