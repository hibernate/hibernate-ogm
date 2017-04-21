/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.utils;

import java.util.Set;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.orientdb.OrientDBProperties;
import org.hibernate.ogm.datastore.orientdb.OrientDBProperties.DatabaseTypeEnum;
import org.hibernate.ogm.datastore.orientdb.OrientDBProperties.StorageModeEnum;
import org.hibernate.ogm.datastore.orientdb.constant.OrientDBConstant;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

public class PropertyReaderUtil {

	private static Log log = LoggerFactory.getLogger();

	public static String readHostProperty(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( OgmProperties.HOST, String.class )
				.withDefault( "localhost" )
				.getValue();
	}

	public static String readDatabaseProperty(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( OgmProperties.DATABASE, String.class )
				.getValue();
	}

	public static String readDatabasePathProperty(ConfigurationPropertyReader propertyReader) {

		return propertyReader.property( OrientDBProperties.PLOCAL_PATH, String.class )
				.withDefault( "./target" )
				.getValue();
	}

	public static String readUserProperty(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( OrientDBProperties.USERNAME, String.class )
				.getValue();
	}

	public static String readPasswordProperty(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( OrientDBProperties.PASSWORD, String.class )
				.getValue();
	}

	public static String readDateFormatProperty(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( OrientDBProperties.DATE_FORMAT, String.class )
				.withDefault( OrientDBConstant.DEFAULT_DATE_FORMAT )
				.getValue();
	}

	public static String readDateTimeFormatProperty(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( OrientDBProperties.DATETIME_FORMAT, String.class )
				.withDefault( OrientDBConstant.DEFAULT_DATETIME_FORMAT )
				.getValue();
	}

	public static Integer readPoolSizeProperty(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( OrientDBProperties.POOL_SIZE, Integer.class )
				.withDefault( 10 )
				.getValue();
	}

	public static Boolean readCreateDatabaseProperty(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( OgmProperties.CREATE_DATABASE, Boolean.class )
				.withDefault( Boolean.FALSE )
				.getValue();
	}

	public static StorageModeEnum readStorateModeProperty(ConfigurationPropertyReader propertyReader, StorageModeEnum defaultStorage,
			Set<StorageModeEnum> availableStorages) {
		StorageModeEnum storage = propertyReader.property( OrientDBProperties.STORAGE_MODE_TYPE, OrientDBProperties.StorageModeEnum.class )
				.withDefault( defaultStorage )
				.getValue();
		if ( !availableStorages.contains( storage ) ) {
			// user set unsupportable storage
			throw log.unsupportedStorage( storage );
		}
		return storage;
	}

	public static DatabaseTypeEnum readDatabaseTypeProperty(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( OrientDBProperties.DATEBASE_TYPE, DatabaseTypeEnum.class )
				.withDefault( DatabaseTypeEnum.DOCUMENT )
				.getValue();
	}

}
