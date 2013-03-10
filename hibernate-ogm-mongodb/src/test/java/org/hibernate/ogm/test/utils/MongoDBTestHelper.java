/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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

import com.mongodb.MongoException;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.mongodb.AssociationStorage;
import org.hibernate.ogm.datastore.mongodb.Environment;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.mongodb.MongoDBDialect;
import org.hibernate.ogm.grid.EntityKey;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import org.hibernate.ogm.logging.mongodb.impl.Log;
import org.hibernate.ogm.logging.mongodb.impl.LoggerFactory;

/**
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public class MongoDBTestHelper implements TestableGridDialect {

	private static final Log log = LoggerFactory.getLogger();

	static {
		// Read host and port from environment variable
		// Maven's surefire plugin set it to the string 'null'
		String mongoHostName = System.getenv( "MONGODB_HOSTNAME" );
		if ( isNotNull( mongoHostName ) ) {
			System.getProperties().setProperty( Environment.MONGODB_HOST, mongoHostName );
		}
		String mongoPort = System.getenv( "MONGODB_PORT" );
		if ( isNotNull( mongoPort ) ) {
			System.getProperties().setProperty( Environment.MONGODB_PORT, mongoPort );
		}
	}

	private static boolean isNotNull(String mongoHostName) {
		return mongoHostName != null && mongoHostName.length() > 0 && ! "null".equals( mongoHostName );
	}

	@Override
	public boolean assertNumberOfEntities(int numberOfEntities, SessionFactory sessionFactory) {
		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( sessionFactory );
		AssociationStorage storage = provider.getAssociationStorage();
		DB db = provider.getDatabase();
		int count = 0;
		for ( String collectionName : db.getCollectionNames() ) {
			if ( collectionName.startsWith( "system." ) )
				continue;

			if ( storage == AssociationStorage.GLOBAL_COLLECTION
					&& collectionName.equals( Environment.MONGODB_DEFAULT_ASSOCIATION_STORE ) )
				continue;

			if ( storage == AssociationStorage.COLLECTION
					&& collectionName.startsWith( MongoDBDialect.ASSOCIATIONS_COLLECTION_PREFIX ) )
				continue;


			count += db.getCollection( collectionName ).count();
		}
		return count == numberOfEntities;
	}

	@Override
	public boolean assertNumberOfAssociations(int numberOfAssociations, SessionFactory sessionFactory) {
		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( sessionFactory );
		AssociationStorage assocStorage = provider.getAssociationStorage();
		DB db = provider.getDatabase();

		if ( assocStorage == AssociationStorage.IN_ENTITY ) {
			return true; //FIXME find a way to test that, maybe with some map reduce magic?
		}
		else if ( assocStorage == AssociationStorage.GLOBAL_COLLECTION ) {
			return db.getCollection( Environment.MONGODB_DEFAULT_ASSOCIATION_STORE ).count() == numberOfAssociations;
		}
		else if ( assocStorage == AssociationStorage.COLLECTION ) {
			int count = 0;
			for ( String collectionName : db.getCollectionNames() ) {
				if ( assocStorage == AssociationStorage.COLLECTION
						&& collectionName.startsWith( MongoDBDialect.ASSOCIATIONS_COLLECTION_PREFIX ) ) {
					count += db.getCollection( collectionName ).count();
				}
			}
			return count == numberOfAssociations;
		}
		else {
			throw new AssertionFailure( "Unknown AssociationStorage approach: " + assocStorage );
		}
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		MongoDBDatastoreProvider provider = MongoDBTestHelper.getProvider( sessionFactory );
		DBObject finder = new BasicDBObject( MongoDBDialect.ID_FIELDNAME, key.getColumnValues()[0] );
		DBObject result = provider.getDatabase().getCollection( key.getTable() ).findOne( finder );
		replaceIdentifierColumnName( result, key );
		return result.toMap();
	}

	/**
	 * The MongoDB dialect replaces the name of the column identifier, so when the tuple is extracted from the db
	 * we replace the column name of the identifier with the original one.
	 * We are assuming the identifier is not embedded and is a single property.
	 */
	private void replaceIdentifierColumnName(DBObject result, EntityKey key) {
		Object idValue = result.get( MongoDBDialect.ID_FIELDNAME );
		result.removeField( MongoDBDialect.ID_FIELDNAME );
		result.put( key.getColumnNames()[0], idValue );
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	private static MongoDBDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService(
				DatastoreProvider.class );
		if ( !( MongoDBDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with MongoDB, cannot extract underlying cache" );
		}
		return MongoDBDatastoreProvider.class.cast( provider );
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		MongoDBDatastoreProvider provider = getProvider( sessionFactory );
		try {
			provider.getDatabase().dropDatabase();
		}
		catch ( MongoException ex ) {
			throw log.unableToDropDatabase( ex, provider.getDatabase().getName() );
		}
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		//read variables from the System properties set in the static initializer
		Map<String,String> envProps = new HashMap<String, String>(2);
		copyFromSystemPropertiesToLocalEnvironment( Environment.MONGODB_HOST, envProps );
		copyFromSystemPropertiesToLocalEnvironment( Environment.MONGODB_PORT, envProps );
		copyFromSystemPropertiesToLocalEnvironment( Environment.MONGODB_USERNAME, envProps );
		copyFromSystemPropertiesToLocalEnvironment( Environment.MONGODB_PASSWORD, envProps );
		return envProps;
	}

	private void copyFromSystemPropertiesToLocalEnvironment(String environmentVariableName, Map<String, String> envProps) {
		String value = System.getProperties().getProperty( environmentVariableName );
		if ( value != null && value.length() > 0 ) {
			envProps.put( environmentVariableName, value );
		}
	}

}
