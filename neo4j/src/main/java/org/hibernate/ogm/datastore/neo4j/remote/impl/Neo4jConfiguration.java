/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.impl;

import org.hibernate.ogm.cfg.spi.DataStoreConfiguration;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * @author Davide D'Alto
 */
public class Neo4jConfiguration extends DataStoreConfiguration {

	public static final int DEFAULT_PORT = 7474;

	public Neo4jConfiguration(ConfigurationPropertyReader propertyReader) {
		super( propertyReader, DEFAULT_PORT );
	}
}
