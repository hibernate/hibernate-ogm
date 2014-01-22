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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.couchdb.CouchDB;
import org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastore;
import org.hibernate.ogm.datastore.couchdb.impl.CouchDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.couchdb.impl.backend.json.EntityDocument;
import org.hibernate.ogm.dialect.couchdb.impl.model.CouchDBTupleSnapshot;
import org.hibernate.ogm.dialect.couchdb.impl.util.Identifier;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.options.generic.document.AssociationStorageType;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

/**
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class CouchDBTestHelper implements TestableGridDialect {

	static {
		RegisterBuiltin.register( ResteasyProviderFactory.getInstance() );
	}

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		return getDataStore( sessionFactory ).getNumberOfEntities();
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		Integer count = getDataStore( sessionFactory ).getNumberOfAssociations().get( type );
		return count != null ? count : 0;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		CouchDBDatastore dataStore = getDataStore( sessionFactory );

		Map<AssociationStorageType, Integer> associationCountByType = dataStore.getNumberOfAssociations();
		int totalCount = 0;
		for ( int count : associationCountByType.values() ) {
			totalCount += count;
		}
		return totalCount;
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		Map<String, Object> tupleMap = new HashMap<String, Object>();
		CouchDBDatastore dataStore = getDataStore( sessionFactory );
		EntityDocument entity = dataStore.getEntity( Identifier.createEntityId( key ) );
		CouchDBTupleSnapshot snapshot = new CouchDBTupleSnapshot( entity.getProperties() );
		Set<String> columnNames = snapshot.getColumnNames();
		for ( String columnName : columnNames ) {
			tupleMap.put( columnName, snapshot.get( columnName ) );
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
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.HOST, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.PORT, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.DATABASE, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.USERNAME, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.PASSWORD, envProps );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.CREATE_DATABASE, envProps );
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

	@Override
	public GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration) {
		return configuration.configureOptionsFor( CouchDB.class );
	}
}
