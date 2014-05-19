/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.impl;

import org.hibernate.ogm.cfg.impl.DocumentStoreConfiguration;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;

/**
 * Provides utility methods to access the CouchDB configuration value
 *
 * @author Andrea Boriero <dreborier@gmail.com>
 * @author Gunnar Morling
 */
public class CouchDBConfiguration extends DocumentStoreConfiguration {

	public static final int DEFAULT_PORT = 5984;

	public CouchDBConfiguration(ConfigurationPropertyReader reader) {
		super( reader, DEFAULT_PORT );
	}
}
