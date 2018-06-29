/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.impl;

import org.hibernate.ogm.datastore.mongodb.options.ReadConcern;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.OptionValuePair;

/**
 * Converts {@link ReadConcern} instances into an equivalent option value pair.
 *
 * @author Aleksandr Mylnikov
 */
public class ReadConcernConverter implements AnnotationConverter<ReadConcern> {

	@Override
	public OptionValuePair<?> convert(ReadConcern annotation) {
		com.mongodb.ReadConcern readConcern = null;
		readConcern = annotation.value().getReadConcern();
		return OptionValuePair.getInstance( new ReadConcernOption(), readConcern );
	}

}
