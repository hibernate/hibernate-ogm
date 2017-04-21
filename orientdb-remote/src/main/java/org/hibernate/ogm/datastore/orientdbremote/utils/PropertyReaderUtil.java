/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdbremote.utils;

import org.hibernate.ogm.datastore.orientdbremote.RemoteOrientDBProperties;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

public class PropertyReaderUtil extends org.hibernate.ogm.datastore.orientdb.utils.PropertyReaderUtil {

	public static String readRootUserProperty(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( RemoteOrientDBProperties.ROOT_USERNAME, String.class ).withDefault( "root" ).getValue();
	}

	public static String readRootPasswordProperty(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( RemoteOrientDBProperties.ROOT_PASSWORD, String.class ).withDefault( "root" ).getValue();
	}

}
