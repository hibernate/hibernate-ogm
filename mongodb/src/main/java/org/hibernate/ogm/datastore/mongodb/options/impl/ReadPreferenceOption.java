/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.impl;

import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

import com.mongodb.ReadPreference;

/**
 * Option for specifying the <a href="http://docs.mongodb.org/manual/core/read-preference/">read preference</a> in
 * MongoDB.
 *
 * @author Gunnar Morling
 */
public class ReadPreferenceOption extends UniqueOption<ReadPreference> {

	/**
	 * The default read preference.
	 */
	private static final ReadPreferenceType DEFAULT_READ_PREFERENCE = ReadPreferenceType.PRIMARY;

	@Override
	public ReadPreference getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return propertyReader
				.property( MongoDBProperties.READ_PREFERENCE, ReadPreferenceType.class )
				.withDefault( DEFAULT_READ_PREFERENCE )
				.getValue()
				.getReadPreference();
	}
}
