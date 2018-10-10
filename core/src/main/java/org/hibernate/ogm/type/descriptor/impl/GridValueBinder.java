/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Contract to bind a value to the resultset
 *
 * @author Emmanuel Bernard
 */
public interface GridValueBinder<X> {

	void bind(Tuple resultset, X value, String[] names);

	default void bind(Tuple rs, X value, String[] names, WrapperOptions options) {
		bind( rs, value, names );
	}
}
