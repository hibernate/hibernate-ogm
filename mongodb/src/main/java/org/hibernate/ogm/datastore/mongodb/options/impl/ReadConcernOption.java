/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.impl;

import com.mongodb.ReadConcern;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.options.ReadConcernType;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Option for specifying the <a href="https://docs.mongodb.com/manual/reference/read-concern/">read concern</a> in
 * MongoDB.
 *
 * @author Aleksandr Mylnikov
 */
public class ReadConcernOption extends UniqueOption<ReadConcern> {

	@Override
	public ReadConcern getDefaultValue(ConfigurationPropertyReader propertyReader) {
		ReadConcernType readConcernType = propertyReader.property( MongoDBProperties.READ_CONCERN, ReadConcernType.class )
				.withDefault( ReadConcernType.DEFAULT )
				.getValue();
		return readConcernType.getReadConcern();
	}
}
