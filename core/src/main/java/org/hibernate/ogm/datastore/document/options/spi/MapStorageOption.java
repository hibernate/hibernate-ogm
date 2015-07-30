/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.options.spi;

import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.MapStorageType;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Option for specifying the strategy for storing map contents in document stores.
 *
 * @author Gunnar Morling
 */
public class MapStorageOption extends UniqueOption<MapStorageType> {

	/**
	 * The default map storage strategy.
	 */
	private static final MapStorageType DEFAULT_MAP_STORAGE = MapStorageType.BY_KEY;

	@Override
	public MapStorageType getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return propertyReader
				.property( DocumentStoreProperties.MAP_STORAGE, MapStorageType.class )
				.withDefault( DEFAULT_MAP_STORAGE )
				.getValue();
	}
}
