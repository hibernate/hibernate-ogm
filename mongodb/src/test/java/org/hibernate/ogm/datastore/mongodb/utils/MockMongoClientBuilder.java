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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;

/**
 * A builder for mocked {@link MongoClient} instances which return given {@link Document}s for given collections.
 * <p>
 * Note that currently only one {@code Document} is allowed per collection, but this could be expanded into a more
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

		private final Map<String, MongoCollection<Document>> collections = new HashMap<>();

		/**
		 * Registers the given {@link Document} with the specified collection. The object can be retrieved from the
		 * collection via {@link MongoCollection<Document>#findOne(Document, Document))}.
		 * <p>
		 * Note that currently only one {@code Document} is supported per collection, but this could be expanded into a
		 * more general mechanism if required.
		 */
		public MockMongoClientBuilderContext insert(String collectionName, Document object) {
			MongoCollection<Document> collection = mock( MongoCollection.class );
			collections.put( collectionName, collection );

			when( collection.find( any( Document.class ) ).projection( any( Document.class ) ).first() ).thenReturn( object );

			DeleteResult deleteResult = mock( DeleteResult.class );
			when( collection.deleteMany( any( Document.class ) ) ).thenReturn( deleteResult );

			return this;
		}

		/**
		 * Builds and returns a mock MongoDB client based on the given configuration.
		 */
		public MockMongoClient build() {
			MongoDatabase database = mock( MongoDatabase.class );

			MongoCollection<Document> defaultCollection = mock( MongoCollection.class );
			when( database.getCollection( anyString() ) ).thenReturn( defaultCollection );

			for ( Entry<String, MongoCollection<Document>> collection : collections.entrySet() ) {
				when( database.getCollection( collection.getKey() ) ).thenReturn( collection.getValue() );
			}

			MongoClient mongoClient = mock( MongoClient.class );
			//@TODO prepare mock for MongoCursor
			//when( mongoClient.getDatabaseNames() ).thenReturn( Collections.<String>emptyList() );
			when( mongoClient.getDatabase( anyString() ) ).thenReturn( database );

			return new MockMongoClient( collections, defaultCollection, mongoClient );
		}
	}

	/**
	 * A mock client for MongoDB.
	 *
	 * @author Gunnar Morling
	 */
	public static class MockMongoClient {

		private final Map<String, MongoCollection<Document>> collections;
		private final MongoCollection<Document> defaultCollection;
		private final MongoClient client;

		public MockMongoClient(Map<String, MongoCollection<Document>> collections, MongoCollection<Document> defaultCollection, MongoClient client) {
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
		public MongoCollection<Document> getCollection(String collectionName) {
			MongoCollection<Document> collection = collections.get( collectionName );
			return collection != null ? collection : defaultCollection;
		}
	}
}
