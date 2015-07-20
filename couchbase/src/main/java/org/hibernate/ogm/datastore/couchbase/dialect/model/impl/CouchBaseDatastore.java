/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchbase.dialect.model.impl;

import static org.hibernate.ogm.datastore.document.util.impl.Identifier.createSourceId;

import org.hibernate.ogm.datastore.couchbase.logging.impl.Log;
import org.hibernate.ogm.datastore.couchbase.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.couchbase.util.impl.DatabaseIdentifier;
import org.hibernate.ogm.model.key.spi.IdSourceKey;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.JsonLongDocument;

/**
 * Interacts with CouchBase Server, manage connections, store documents.
 * <p>
 * Tuples are stored as {@link JsonDocument}.
 * Next values are stored in {@link JsonLongDocument}.
 * @author Stawicka Ewa
 */
public class CouchBaseDatastore {

	private static final Log logger = LoggerFactory.make();

	private CouchbaseCluster client;
	private final DatabaseIdentifier database;

	private Bucket bucket;

	private CouchBaseDatastore(DatabaseIdentifier database) {
		this.database = database;
	}

	/**
	 * Creates an instance of CouchBaseDatastore.
	 *
	 * @param database a handle to the database
	 * @param createDatabase if true the database is created
	 * @return an instance of CouchBaseDatastore
	 */
	public static CouchBaseDatastore newInstance(DatabaseIdentifier database, boolean createDatabase) {

		CouchBaseDatastore couchBasedatastore = new CouchBaseDatastore( database );
		couchBasedatastore.initialize( createDatabase );

		return couchBasedatastore;
	}

	private void initialize(boolean createDatabase) {
		try {
			logger.connectingToCouchBase( database.getDatabaseUri().toString() );
			client = CouchbaseCluster.create( database.getHost() );
			bucket = client.openBucket( database.getDatabaseName(), database.getPassword() );
		}
		catch (Exception e) {
			throw logger.couchBaseConnectionProblem( e );
		}
	}

	/**
	 * Saves a Document to the database
	 */
	public void saveDocument(JsonDocument jsonObject) {
		bucket.upsert( jsonObject );
	}

	/**
	 * Releases all the resources
	 */
	public void shutDown() {
		if (client != null) {
			client.disconnect();
		}
	}

	public JsonDocument getEntity(String createEntityId) {
		return bucket.get( createEntityId );
	}

	public void deleteDocument(String id) {
		bucket.remove( id );
	}

	/**
	 * Store each sequence as separate document
	 */
	public Number nextValue(IdSourceKey key, int increment, int initialValue) {
		return bucket.counter( createSourceId( key ), increment, initialValue ).content();
	}

}
