/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchbase.impl;
import org.hibernate.ogm.cfg.spi.DocumentStoreConfiguration;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
/**
 * Provides utility methods to access the CouchBase configuration value
 *
 * @author Stawicka Ewa
 */
public class CouchBaseConfiguration extends DocumentStoreConfiguration {
	public static final int DEFAULT_PORT = 8091;
	public CouchBaseConfiguration(ConfigurationPropertyReader propertyReader) {
		super( propertyReader, DEFAULT_PORT );
	}
}
