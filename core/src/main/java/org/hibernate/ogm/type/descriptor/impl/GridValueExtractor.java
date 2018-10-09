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
 * Extract value from the result set
 *
 * @author Emmanuel Bernard
 */
public interface GridValueExtractor<X> {

	X extract(Tuple resultset, String name);

	default X extract(Tuple resultset, String name, WrapperOptions options) {
		return extract( resultset, name );
	}
}
