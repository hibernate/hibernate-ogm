/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.shared.impl;

import org.hibernate.ogm.options.shared.IndexOptions;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.OptionValuePair;

/**
 * Converts {@link IndexOptions} instances into an equivalent option value pair.
 *
 * @author Guillaume Smet
 */
public class IndexOptionsConverter implements AnnotationConverter<IndexOptions> {

	@Override
	public OptionValuePair<?> convert(IndexOptions annotation) {
		return OptionValuePair.getInstance( new IndexOptionsOption(), new org.hibernate.ogm.options.shared.spi.IndexOptions( annotation ) );
	}
}
