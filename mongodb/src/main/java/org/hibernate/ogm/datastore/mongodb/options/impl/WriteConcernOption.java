/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.impl;

import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

import com.mongodb.WriteConcern;

/**
 * Option for specifying the <a href="http://docs.mongodb.org/manual/reference/write-concern/">write concern</a> in
 * MongoDB.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class WriteConcernOption extends UniqueOption<WriteConcern> {

	/**
	 * The default write concern.
	 *
	 * @see MongoDBProperties#WRITE_CONCERN
	 */
	private static final WriteConcernType DEFAULT_WRITE_CONCERN = WriteConcernType.ACKNOWLEDGED;

	@Override
	public WriteConcern getDefaultValue(ConfigurationPropertyReader propertyReader) {
		WriteConcernType writeConcernType = propertyReader.property( MongoDBProperties.WRITE_CONCERN, WriteConcernType.class )
			.withDefault( DEFAULT_WRITE_CONCERN )
			.getValue();

		// load/instantiate custom type
		if ( writeConcernType == WriteConcernType.CUSTOM ) {
			return propertyReader.property( MongoDBProperties.WRITE_CONCERN_TYPE, WriteConcern.class )
				.instantiate()
				.required()
				.getValue();
		}
		// take pre-defined value
		else {
			return writeConcernType.getWriteConcern();
		}
	}
}
