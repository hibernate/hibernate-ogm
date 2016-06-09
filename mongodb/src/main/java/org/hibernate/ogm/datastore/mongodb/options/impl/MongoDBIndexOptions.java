/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.impl;


/**
 * MongoDB specific options for a given index.
 *
 * See <a href="https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/#options-for-all-index-types">https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/#options-for-all-index-types</a>
 *
 * @author Guillaume Smet
 */
public class MongoDBIndexOptions {

	/**
	 * The target index name.
	 */
	private String targetIndexName;

	/**
	 * Builds the index in the background so that building an index does not block other database activities.
	 */
	private boolean background;

	/**
	 * Allows to define a partial index by providing a filter expression.
	 *
	 * Must be a valid MongoDB document.
	 */
	private String partialFilterExpression;

	/**
	 * If true, the index only references documents with the specified field.
	 * These indexes use less space but behave differently in some situations (particularly sorts).
	 * The default value is false
	 */
	private boolean sparse;

	/**
	 * Specifies a value, in seconds, as a TTL to control
	 * how long MongoDB retains documents in this collection
	 */
	private int expireAfterSeconds;

	/**
	 * Allows to define options for the storage engine.
	 *
	 * Must be a valid MongoDB document.
	 */
	private String storageEngine;

	/**
	 * Specific options for full text indexes.
	 *
	 * Note that MongoDB only supports one full text index per collection.
	 */
	private MongoDBTextIndexOptions text;

	MongoDBIndexOptions() {
	}

	MongoDBIndexOptions(String targetIndexName) {
		this.targetIndexName = targetIndexName;
	}

	MongoDBIndexOptions(org.hibernate.ogm.datastore.mongodb.options.MongoDBIndexOptions annotation) {
		this.targetIndexName = annotation.forIndex();
		this.background = annotation.background();
		this.partialFilterExpression = annotation.partialFilterExpression();
		this.sparse = annotation.sparse();
		this.expireAfterSeconds = annotation.expireAfterSeconds();
		this.storageEngine = annotation.storageEngine();
		if ( annotation.text().length > 0 ) {
			this.text = new MongoDBTextIndexOptions( annotation.text()[0] );
		}
	}

	public String getTargetIndexName() {
		return targetIndexName;
	}

	public boolean isBackground() {
		return background;
	}

	public String getPartialFilterExpression() {
		return partialFilterExpression;
	}

	public boolean isSparse() {
		return sparse;
	}

	public int getExpireAfterSeconds() {
		return expireAfterSeconds;
	}

	public String getStorageEngine() {
		return storageEngine;
	}

	public MongoDBTextIndexOptions getText() {
		return text;
	}

}
