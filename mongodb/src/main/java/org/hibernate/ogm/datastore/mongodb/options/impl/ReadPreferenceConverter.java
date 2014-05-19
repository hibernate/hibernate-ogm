/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.impl;

import org.hibernate.ogm.datastore.mongodb.options.ReadPreference;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.OptionValuePair;

/**
 * Converts {@link ReadPreference} instances into an equivalent option value pair.
 *
 * @author Gunnar Morling
 */
public class ReadPreferenceConverter implements AnnotationConverter<ReadPreference> {

	@Override
	public OptionValuePair<?> convert(ReadPreference annotation) {
		return OptionValuePair.getInstance( new ReadPreferenceOption(), annotation.value().getReadPreference() );
	}
}
