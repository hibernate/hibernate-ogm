/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.utils;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * A builder for mocked {@link MongoClient} instances which return given {@link DBObject}s for given collections.
 * <p>
 * Note that currently only one {@code DBObject} is allowed per collection, but this could be expanded into a more
 * general mechanism if required.
 *
 * @author Gunnar Morling
 */
public class MockMongoClientBuilder {

	/**
	 * Builds an new mock MongoDB client.
	 *
	 * @return a builder context following the fluent invocation pattern.
	 */
	public static MockMongoClientBuilderContext mockClient() {
		return new MockMongoClientBuilderContext();
	}

	public static class MockMongoClientBuilderContext {

		private final Map<String, DBCollection> collections = new HashMap<String, DBCollection>();

		/**
		 * Registers the given {@link DBObject} with the specified collection. The object can be retrieved from the
		 * collection via {@link DBCollection#findOne(DBObject, DBObject))}.
		 * <p>
		 * Note that currently only one {@code DBObject} is supported per collection, but this could be expanded into a
		 * more general mechanism if required.
		 */
		public MockMongoClientBuilderContext insert(String collectionName, DBObject object) {
			DBCollection collection = mock( DBCollection.class );
			collections.put( collectionName, collection );

			when( collection.findOne( any( DBObject.class ), any( DBObject.class ), any( ReadPreference.class ) ) ).thenReturn( object );

			WriteResult writeResult = mock( WriteResult.class );
			when( collection.remove( any( DBObject.class ), any( WriteConcern.class ) ) ).thenReturn( writeResult );

			return this;
		}

		/**
		 * Builds and returns a mock MongoDB client based on the given configuration.
		 */
		public MockMongoClient build() {
			DB database = mock( DB.class );

			DBCollection defaultCollection = mock( DBCollection.class );
			when( database.getCollection( anyString() ) ).thenReturn( defaultCollection );

			for ( Entry<String, DBCollection> collection : collections.entrySet() ) {
				when( database.getCollection( collection.getKey() ) ).thenReturn( collection.getValue() );
			}

			MongoClient mongoClient = mock( MongoClient.class );
			when( mongoClient.getDatabaseNames() ).thenReturn( Collections.<String>emptyList() );
			when( mongoClient.getDB( anyString() ) ).thenReturn( database );

			return new MockMongoClient( collections, defaultCollection, mongoClient );
		}
	}

	/**
	 * A mock client for MongoDB.
	 *
	 * @author Gunnar Morling
	 */
	public static class MockMongoClient {

		private final Map<String, DBCollection> collections;
		private final DBCollection defaultCollection;
		private final MongoClient client;

		public MockMongoClient(Map<String, DBCollection> collections, DBCollection defaultCollection, MongoClient client) {
			this.collections = collections;
			this.defaultCollection = defaultCollection;
			this.client = client;
		}

		/**
		 * Returns a mock {@link MongoClient}.
		 */
		public MongoClient getClient() {
			return client;
		}

		/**
		 * Returns the collection with a given name. This is a Mockito mock object, so verifications can be performed on
		 * it.
		 */
		public DBCollection getCollection(String collectionName) {
			DBCollection collection = collections.get( collectionName );
			return collection != null ? collection : defaultCollection;
		}
	}
}
