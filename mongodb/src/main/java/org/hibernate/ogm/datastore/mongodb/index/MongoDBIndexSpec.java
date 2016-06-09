/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.index;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.ogm.datastore.mongodb.options.impl.MongoDBIndexOptions;
import org.hibernate.ogm.datastore.mongodb.options.impl.MongoDBTextIndexOptions;
import org.hibernate.ogm.util.impl.StringHelper;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Definition of an index to be applied to a MongoDB collection
 *
 * @author Francois Le Droff
 * @author Guillaume Smet
 */
public class MongoDBIndexSpec {

	/**
	 * The MongoDB collection/table for which the index will be set
	 */
	private String collection;

	/**
	 * A unique index causes MongoDB to reject all documents that contain a duplicate value for the indexed field.
	 * @see http://docs.mongodb.org/manual/core/index-unique/
	 * true if the indexed field is bound to be unique within the collection
	 */
	private boolean unique;

	/**
	 * Optional. The name of the index. If unspecified, MongoDB generates an index name
	 * by concatenating the names of the indexed fields and the sort order.
	 *
	 * Whether user specified or MongoDB generated, index names including their full namespace (i.e. database.collection)
	 * cannot be longer than the Index Name Limit (that is 128 characters)
	 */
	private String indexName;

	/**
	 * The index keys of the index prepared for MongoDB.
	 */
	private DBObject indexKeys = new BasicDBObject();

	/**
	 * The options specific to MongoDB.
	 */
	private MongoDBIndexOptions options;

	/**
	 * Constructor used for columns marked as unique.
	 */
	public MongoDBIndexSpec(String collection, String columnName, String indexName, MongoDBIndexOptions options) {
		this.options = options;
		this.collection = collection;
		this.indexName = indexName;
		this.unique = true;
		indexKeys.put( columnName, 1 );
	}

	/**
	 * Constructor used for {@link UniqueKey}s.
	 */
	public MongoDBIndexSpec(UniqueKey uniqueKey, MongoDBIndexOptions options) {
		this.options = options;
		this.collection = uniqueKey.getTable().getName();
		this.indexName = uniqueKey.getName();
		this.unique = true;
		this.addIndexKeys( uniqueKey.getColumnIterator(), uniqueKey.getColumnOrderMap() );
	}

	/**
	 * Constructor used for {@link Index}es.
	 */
	public MongoDBIndexSpec(Index index, MongoDBIndexOptions options) {
		this.options = options;
		this.collection = index.getTable().getName();
		this.indexName = index.getName();
		this.unique = false;
		// TODO OGM-1080: the columnOrderMap is not accessible for an Index
		this.addIndexKeys( index.getColumnIterator(), Collections.<Column, String>emptyMap() );
	}

	public String getCollection() {
		return collection;
	}

	public String getIndexName() {
		return indexName;
	}

	public boolean isTextIndex() {
		return options.getText() != null;
	}

	private void addIndexKeys(Iterator<Column> columnIterator, Map<Column, String> columnOrderMap) {
		while (columnIterator.hasNext()) {
			Column column = columnIterator.next();
			Object mongoDBOrder;
			if ( options.getText() != null ) {
				mongoDBOrder = "text";
			}
			else {
				String order = columnOrderMap.get( column ) != null ? columnOrderMap.get( column ) : "asc";
				mongoDBOrder = "asc".equals( order ) ? 1 : -1;
			}

			indexKeys.put( column.getName(), mongoDBOrder );
		}
	}

	public MongoDBIndexOptions getIndexOption() {
		return options;
	}

	public DBObject getIndexKeysDBObject() {
		return indexKeys;
	}

	public DBObject getIndexOptionsDBObject() {
		DBObject dbo = new BasicDBObject();
		if ( !StringHelper.isNullOrEmptyString( indexName ) ) {
			dbo.put( "name", indexName );
		}
		if ( unique ) {
			dbo.put( "unique", true );
		}
		if ( options.isSparse() ) {
			dbo.put( "sparse", true );
		}
		if ( options.isBackground() ) {
			dbo.put( "background", true );
		}
		if ( options.getExpireAfterSeconds() >= 0 ) {
			dbo.put( "expireAfterSeconds", options.getExpireAfterSeconds() );
		}
		if ( !StringHelper.isNullOrEmptyString( options.getPartialFilterExpression() ) ) {
			dbo.put( "partialFilterExpression", BasicDBObject.parse( options.getPartialFilterExpression() ) );
		}
		if ( !StringHelper.isNullOrEmptyString( options.getStorageEngine() ) ) {
			dbo.put( "storageEngine", BasicDBObject.parse( options.getStorageEngine() ) );
		}
		if ( options.getText() != null ) {
			MongoDBTextIndexOptions textOptions = options.getText();
			if ( !StringHelper.isNullOrEmptyString( textOptions.getDefaultLanguage() ) ) {
				dbo.put( "default_language", textOptions.getDefaultLanguage() );
			}
			if ( !StringHelper.isNullOrEmptyString( textOptions.getLanguageOverride() ) ) {
				dbo.put( "language_override", textOptions.getLanguageOverride() );
			}
			if ( !StringHelper.isNullOrEmptyString( textOptions.getWeights() ) ) {
				dbo.put( "weights", BasicDBObject.parse( textOptions.getWeights() ) );

			}
		}
		return dbo;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( getClass().getSimpleName() );
		sb.append( "[" );
		sb.append( "collection: " ).append( collection ).append( ", " );
		sb.append( "indexName: " ).append( indexName );
		sb.append( "]" );
		return sb.toString();
	}

}
