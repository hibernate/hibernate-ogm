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
package org.hibernate.ogm.test.utils;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastore;
import org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.couchdb.Environment;
import org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBEntity;
import org.hibernate.ogm.dialect.couchdb.util.Identifier;
import org.hibernate.ogm.grid.EntityKey;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class CouchDBTestHelper implements TestableGridDialect {

	static {
		RegisterBuiltin.register( ResteasyProviderFactory.getInstance() );
	}

	@Override
	public boolean assertNumberOfEntities(int numberOfEntities, SessionFactory sessionFactory) {
		CouchDBDatastore dataStore = getDataStore( sessionFactory );
		return dataStore.getNumberOfEntities() == numberOfEntities;
	}

	@Override
	public boolean assertNumberOfAssociations(int numberOfAssociations, SessionFactory sessionFactory) {
		CouchDBDatastore dataStore = getDataStore( sessionFactory );
		return dataStore.getNumberOfAssociations() == numberOfAssociations;
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		Map<String, Object> tupleMap = new HashMap<String, Object>();
		CouchDBDatastore dataStore = getDataStore( sessionFactory );
		CouchDBEntity entity = dataStore.getEntity( new Identifier().createEntityId( key ) );
		String[] columnNames = entity.getTuple().getColumnNames();
		Object[] columnValues = entity.getTuple().getColumnValues();
		int length = columnNames.length;
		for ( int i = 0; i < length; i++ ) {
			tupleMap.put( columnNames[i], columnValues[i] );
		}
		return tupleMap;
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		getDataStore( sessionFactory ).dropDatabase();
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		Map<String, String> envProps = new HashMap<String, String>( 2 );
		copyFromSystemPropertiesToLocalEnvironment( Environment.COUCHDB_HOST, envProps );
		copyFromSystemPropertiesToLocalEnvironment( Environment.COUCHDB_PORT, envProps );
		copyFromSystemPropertiesToLocalEnvironment( Environment.COUCHDB_DATABASE, envProps );
		copyFromSystemPropertiesToLocalEnvironment( Environment.COUCHDB_USERNAME, envProps );
		copyFromSystemPropertiesToLocalEnvironment( Environment.COUCHDB_PASSWORD, envProps );
		copyFromSystemPropertiesToLocalEnvironment( Environment.COUCHDB_CREATE_DATABASE, envProps );
		return envProps;
	}

	private void copyFromSystemPropertiesToLocalEnvironment(String environmentVariableName, Map<String, String> envProps) {
		String value = System.getProperties().getProperty( environmentVariableName );
		if ( value != null && value.length() > 0 ) {
			envProps.put( environmentVariableName, value );
		}
	}

	private CouchDBDatastore getDataStore(SessionFactory sessionFactory) {
		CouchDBDatastoreProvider provider = getProvider( sessionFactory );
		return provider.getDataStore();
	}

	private CouchDBDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService(
				DatastoreProvider.class );
		if ( !( provider instanceof CouchDBDatastoreProvider ) ) {
			throw new RuntimeException( "DatastoreProvider is not an instance of " + CouchDBDatastoreProvider.class );
		}
		return (CouchDBDatastoreProvider) provider;
	}
}
