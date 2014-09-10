/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.impl;

import java.util.Map;

import org.hibernate.ogm.datastore.couchdb.CouchDBDialect;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.impl.CouchDBDatastore;
import org.hibernate.ogm.datastore.couchdb.logging.impl.Log;
import org.hibernate.ogm.datastore.couchdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.couchdb.util.impl.DatabaseIdentifier;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * Creates a fully configured instance of {@link CouchDBDatastore}
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 * @author Gunnar Morling
 */
public class CouchDBDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable, ServiceRegistryAwareService, Configurable {

	private static final Log logger = LoggerFactory.getLogger();

	private CouchDBDatastore datastore;

	private CouchDBConfiguration configuration;

	public CouchDBDatastoreProvider() {
	}

	@Override
	public void configure(Map configurationValues) {
		configuration = new CouchDBConfiguration( new ConfigurationPropertyReader( configurationValues ) );
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
	}

	@Override
	public void stop() {
		if ( datastore != null ) {
			datastore.shutDown();
			datastore = null;
		}
	}

	@Override
	public void start() {
		if ( isDatastoreNotInitialized() ) {
			datastore = CouchDBDatastore.newInstance( getDatabase(), configuration.isCreateDatabase() );
		}
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return CouchDBDialect.class;
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return CouchDBSchemaSchemaDefiner.class;
	}

	/**
	 * Provides an instance of CouchDBDatastore
	 *
	 * @return an instance of CouchDBDatastore
	 */
	public CouchDBDatastore getDataStore() {
		return datastore;
	}

	private boolean isDatastoreNotInitialized() {
		return datastore == null;
	}

	private DatabaseIdentifier getDatabase() {
		try {
			return new DatabaseIdentifier(
					configuration.getHost(),
					configuration.getPort(),
					configuration.getDatabaseName(),
					configuration.getUsername(),
					configuration.getPassword()
			);
		}
		catch (Exception e) {
			throw logger.malformedDataBaseUrl(
					e, configuration.getHost(), configuration.getPort(), configuration.getDatabaseName()
			);
		}
	}

}
