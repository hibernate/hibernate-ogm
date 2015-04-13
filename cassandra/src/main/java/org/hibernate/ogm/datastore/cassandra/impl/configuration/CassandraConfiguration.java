/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.impl.configuration;

import org.hibernate.ogm.cfg.spi.DocumentStoreConfiguration;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Provides utility methods to access the Cassandra configuration values.
 *
 * @author Jonathan Halliday
 */
public class CassandraConfiguration extends DocumentStoreConfiguration {

	private static final int DEFAULT_PORT = 9042;

	public CassandraConfiguration(ConfigurationPropertyReader propertyReader, OptionsContext globalOptions) {
		super( propertyReader, DEFAULT_PORT );
	}
}
