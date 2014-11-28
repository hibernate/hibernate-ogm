/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.keyvalue.options.spi;

import org.hibernate.ogm.datastore.keyvalue.cfg.KeyValueStoreProperties;
import org.hibernate.ogm.datastore.keyvalue.options.CacheStorageType;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Represents the type of cache storage as configured via the API or annotations for a given element.
 *
 * @author Gunnar Morling
 */
public class CacheStorageOption extends UniqueOption<CacheStorageType> {

	private static final CacheStorageType DEFAULT_CACHE_STORAGE = CacheStorageType.CACHE_PER_TABLE;

	@Override
	public CacheStorageType getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( KeyValueStoreProperties.CACHE_STORAGE, CacheStorageType.class )
				.withDefault( DEFAULT_CACHE_STORAGE )
				.getValue();
	}
}
