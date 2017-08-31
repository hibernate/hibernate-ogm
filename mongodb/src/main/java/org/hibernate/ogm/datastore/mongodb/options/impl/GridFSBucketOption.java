/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.impl;

import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class GridFSBucketOption extends UniqueOption<String> {


	@Override
	public String getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return propertyReader
				.property( MongoDBProperties.DEFAULT_GRIDFS_BUCKET_NAME, String.class )
				.getValue();
	}
}
