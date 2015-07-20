/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchbase.impl;

import java.util.Map;

import org.hibernate.ogm.cfg.spi.Hosts.HostAndPort;
import org.hibernate.ogm.datastore.couchbase.CouchBaseDialect;
import org.hibernate.ogm.datastore.couchbase.dialect.model.impl.CouchBaseDatastore;
import org.hibernate.ogm.datastore.couchbase.util.impl.DatabaseIdentifier;
import org.hibernate.ogm.datastore.couchbase.logging.impl.Log;
import org.hibernate.ogm.datastore.couchbase.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * @author Stawicka Ewa
 */
public class CouchBaseDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable, Configurable {

	private static final Log logger = LoggerFactory.make();

	private CouchBaseDatastore datastore;
	private CouchBaseConfiguration configuration;

	public CouchBaseDatastoreProvider() {
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return CouchBaseDialect.class;
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean allowsTransactionEmulation() {
		return true;
	}

	public CouchBaseDatastore getDataStore() {
		return datastore;
	}

	@Override
	public void start() {
		if (isDatastoreNotInitialized()) {
			datastore = CouchBaseDatastore.newInstance( getDatabase(), configuration.isCreateDatabase() );
		}

	}

	@Override
	public void configure(Map configurationValues) {
		configuration = new CouchBaseConfiguration( new ConfigurationPropertyReader( configurationValues ) );

	}

	@Override
	public void stop() {
		if (datastore != null) {
			datastore.shutDown();
			datastore = null;
		}

	}

	private boolean isDatastoreNotInitialized() {
		return datastore == null;
	}

	private DatabaseIdentifier getDatabase() {
		HostAndPort firstHost = configuration.getHosts().getFirst();
		try {
			return new DatabaseIdentifier( firstHost.getHost(), firstHost.getPort(), configuration.getDatabaseName(), configuration.getPassword() );
		}
		catch (Exception e) {
			throw logger.malformedDataBaseUrl( e, firstHost.getHost(), firstHost.getPort(), configuration.getDatabaseName() );
		}

	}

}
