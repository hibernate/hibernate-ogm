/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.impl;

import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcern;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.OptionValuePair;

/**
 * Converts {@link WriteConcern} instances into an equivalent option value pair.
 *
 * @author Gunnar Morling
 */
public class WriteConcernConverter implements AnnotationConverter<WriteConcern> {

	private static final Log log = LoggerFactory.getLogger();

	@Override
	public OptionValuePair<?> convert(WriteConcern annotation) {
		com.mongodb.WriteConcern writeConcern = null;

		if ( annotation.value() == WriteConcernType.CUSTOM ) {
			try {
				writeConcern = annotation.type().newInstance();
			}
			catch (Exception e) {
				throw log.unableToInstantiateType( annotation.type().getName(), e );
			}
		}
		else {
			writeConcern = annotation.value().getWriteConcern();
		}

		return OptionValuePair.getInstance( new WriteConcernOption(), writeConcern );
	}
}
