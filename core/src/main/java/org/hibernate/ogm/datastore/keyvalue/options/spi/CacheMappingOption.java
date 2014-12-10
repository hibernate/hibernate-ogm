/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.keyvalue.options.spi;

import org.hibernate.ogm.datastore.keyvalue.cfg.KeyValueStoreProperties;
import org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Represents the type of cache mapping as configured via the API or annotations for a given element.
 *
 * @author Gunnar Morling
 */
public class CacheMappingOption extends UniqueOption<CacheMappingType> {

	private static final CacheMappingType DEFAULT_CACHE_STORAGE = CacheMappingType.CACHE_PER_TABLE;

	@Override
	public CacheMappingType getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( KeyValueStoreProperties.CACHE_MAPPING, CacheMappingType.class )
				.withDefault( DEFAULT_CACHE_STORAGE )
				.getValue();
	}
}
