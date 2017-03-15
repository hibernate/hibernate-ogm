/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.index.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;

import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * Definition of an index to be applied to a MongoDB collection
 *
 * @author Guillaume Smet
 * @see <a href="https://docs.mongodb.com/manual/indexes/">info about indexes in MongoDB Java Driver 3.4</a>
 */
public class MongoDBIndexSpec {

	private Log log = LoggerFactory.getLogger();

	/**
	 * The MongoDB collection/table for which the index will be set
	 */
	private String collection;

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
	private Document indexKeys = new Document();

	/**
	 * The options specific to MongoDB.
	 */
	private IndexOptions options;

	/**
	 * Indicates if the index is a text index.
	 */
	private boolean isTextIndex = false;

	/**
	 * Constructor used for columns marked as unique.
	 */
	public MongoDBIndexSpec(String collection, String columnName, String indexName, Document options) {
		this.options = prepareOptions( options, indexName, true );
		this.collection = collection;
		this.indexName = indexName;
		indexKeys.put( columnName, 1 );
	}

	/**
	 * Constructor used for {@link UniqueKey}s.
	 */
	public MongoDBIndexSpec(UniqueKey uniqueKey, Document options) {
		this.options = prepareOptions( options, uniqueKey.getName(), true );
		this.collection = uniqueKey.getTable().getName();
		this.indexName = uniqueKey.getName();
		this.addIndexKeys( uniqueKey.getColumnIterator(), uniqueKey.getColumnOrderMap() );
	}

	/**
	 * Constructor used for {@link Index}es.
	 */
	public MongoDBIndexSpec(Index index, Document options) {
		this.options = prepareOptions( options, index.getName(), false );
		this.collection = index.getTable().getName();
		this.indexName = index.getName();
		// TODO OGM-1080: the columnOrderMap is not accessible for an Index
		this.addIndexKeys( index.getColumnIterator(), Collections.<Column, String>emptyMap() );
	}

	/**
	 * Prepare the options by adding additional information to them.
	 * @see <a href="https://docs.mongodb.com/manual/core/index-ttl/"> TTL Indexes</a>
	 */
	private IndexOptions prepareOptions(Document options, String indexName, boolean unique) {
		IndexOptions indexOptions = new IndexOptions();
		indexOptions.name( indexName ).unique( unique ).background( options.getBoolean( "background" , false ) );

		if ( unique ) {
			// MongoDB only allows one null value per unique index which is not in line with what we usually consider
			// as the definition of a unique constraint. Thus, we mark the index as sparse to only index values
			// defined and avoid this issue. We do this only if a partialFilterExpression has not been defined
			// as partialFilterExpression and sparse are exclusive.
			indexOptions.sparse( !options.containsKey( "partialFilterExpression" ) );
		}
		else if ( options.containsKey( "partialFilterExpression" ) ) {
			indexOptions.partialFilterExpression( (Bson) options.get( "partialFilterExpression" ) );
		}
		if ( options.containsKey( "expireAfterSeconds" ) ) {
			//@todo is it correct?
			indexOptions.expireAfter( options.getInteger( "expireAfterSeconds" ).longValue() , TimeUnit.SECONDS );

		}

		if ( Boolean.TRUE.equals( options.get( "text" ) ) ) {
			// text is an option we take into account to mark an index as a full text index as we cannot put "text" as
			// the order like MongoDB as ORM explicitely checks that the order is either asc or desc.
			// we remove the option from the Document so that we don't pass it to MongoDB
			if ( options.containsKey( "default_language" ) ) {
				indexOptions.defaultLanguage( options.getString( "default_language" ) );
			}
			if ( options.containsKey( "weights" ) ) {
				indexOptions.weights( (Bson) options.get( "weights" ) );
			}

			isTextIndex = true;
			options.remove( "text" );
		}
		return indexOptions;
	}

	public String getCollection() {
		return collection;
	}

	public String getIndexName() {
		return indexName;
	}

	public boolean isTextIndex() {
		return isTextIndex;
	}

	private void addIndexKeys(Iterator<Column> columnIterator, Map<Column, String> columnOrderMap) {
		while ( columnIterator.hasNext() ) {
			Column column = columnIterator.next();
			Object mongoDBOrder;
			if ( isTextIndex ) {
				mongoDBOrder = "text";
			}
			else {
				String order = columnOrderMap.get( column ) != null ? columnOrderMap.get( column ) : "asc";
				mongoDBOrder = "asc".equals( order ) ? 1 : -1;
			}

			indexKeys.put( column.getName(), mongoDBOrder );
		}
	}

	public IndexOptions getOptions() {
		return options;
	}

	public Document getIndexKeysDocument() {
		return indexKeys;
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
