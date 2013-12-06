/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.couchdb.impl;

import java.net.MalformedURLException;
import java.util.Map;

import org.hibernate.ogm.datastore.couchdb.impl.util.CouchDBConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.couchdb.CouchDBDialect;
import org.hibernate.ogm.dialect.couchdb.Environment;
import org.hibernate.ogm.dialect.couchdb.util.DataBaseURL;
import org.hibernate.ogm.logging.couchdb.impl.Log;
import org.hibernate.ogm.logging.couchdb.impl.LoggerFactory;
import org.hibernate.ogm.options.couchdb.mapping.impl.CouchDBEntityContextImpl;
import org.hibernate.ogm.options.couchdb.mapping.impl.CouchDBGlobalContextImpl;
import org.hibernate.ogm.options.couchdb.mapping.impl.CouchDBPropertyContextImpl;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.service.impl.LuceneBasedQueryParserService;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * Creates a fully configured instance of {@link CouchDBDatastore}
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class CouchDBDatastoreProvider implements DatastoreProvider, Startable, Stoppable, ServiceRegistryAwareService, Configurable {

	private static final Log logger = LoggerFactory.getLogger();

	private CouchDBDatastore datastore;

	private final CouchDBConfiguration configuration;

	public CouchDBDatastoreProvider() {
		configuration = new CouchDBConfiguration();
	}

	@Override
	public void configure(Map configurationValues) {
		configuration.setConfigurationValues( configurationValues );
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
			datastore = CouchDBDatastore.newInstance( retrieveDataBaseURL(), retrieveUsername(), retrievePassword(), isCreateDatabase() );
		}
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return CouchDBDialect.class;
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return LuceneBasedQueryParserService.class;
	}

	@Override
	public GlobalContext<?, ?> getConfigurationBuilder(ConfigurationContext context) {
		return context.createGlobalContext( CouchDBGlobalContextImpl.class, CouchDBEntityContextImpl.class, CouchDBPropertyContextImpl.class );
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

	private DataBaseURL retrieveDataBaseURL() {
		if ( isDatabaseNameConfigured() ) {
			try {
				return new DataBaseURL( getDatabaseHost(), getDatabasePort(), getDatabaseName() );
			}
			catch (MalformedURLException e) {
				throw logger.malformedDataBaseUrl( e, getDatabaseHost(), getDatabasePort(), getDatabaseName() );
			}
		}
		else {
			throw logger.missingConfigurationProperty( Environment.COUCHDB_DATABASE );
		}
	}

	private boolean isCreateDatabase() {
		return configuration.isDatabaseToBeCreated();
	}

	private String retrievePassword() {
		return configuration.getPassword();
	}

	private String retrieveUsername() {
		return configuration.getUsername();
	}

	private boolean isDatabaseNameConfigured() {
		return configuration.isDatabaseNameConfigured();
	}

	private String getDatabaseName() {
		return configuration.getDatabaseName();
	}

	private String getDatabaseHost() {
		return configuration.getDatabaseHost();
	}

	private int getDatabasePort() {
		return configuration.getDatabasePort();
	}

}
