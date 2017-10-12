/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.impl;

import static org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation.FINDANDMODIFY;
import static org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation.INSERT;
import static org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation.REMOVE;
import static org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation.UPDATE;
import static org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation.UPDATEONE;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.bson.Document;

/**
 * Describes a query to be executed against MongoDB.
 *
 * @author Gunnar Morling
 * @author Davide D'Alto
 * @author Thorsten Möller
 */
public class MongoDBQueryDescriptor implements Serializable {
	/**
	 * Enum with operations
	 * @see <a href="https://docs.mongodb.com/manual/reference/method/js-collection/">list of operations in mongo shell</a>
	 */
	public enum Operation {
		FIND,
		FINDONE,
		FINDANDMODIFY,
		INSERT,
		INSERTONE,
		INSERTMANY,
		REMOVE,
		UPDATE,
		UPDATEONE,
		UPDATEMANY,
		REPLACEONE,
		COUNT,
		/**
		 * This is used by the query parser when the parsed query requires an aggregation, usually for embedded collections.
		 */
		AGGREGATE,
		/**
		 * This is used for native queries, when the user wants to execute a generic aggregation query.
		 */
		AGGREGATE_PIPELINE,
		DISTINCT,
		MAP_REDUCE;
	}

	private final String collectionName;
	private final Operation operation;
	private final Document criteria;   // Overloaded to be the 'document' for a FINDANDMODIFY query (which is a kind of criteria),
	private final Document projection;

	/**
	 * Distinct query will use this field name
	 */
	private final String distinctFieldName;

	private final String mapFunction;
	private final String reduceFunction;

	/**
	 * The "update" (new values to apply) in case this is an UPDATE query or values to insert in case this is an INSERT query.
	 */
	private final Document updateOrInsertOne;
	private final List<Document> updateOrInsertMany;
	private final Document orderBy;

	/**
	 * Optional query options in case this is an UPDATE, INSERT or REMOVE. Will have the following structure:
	 * <ul>
	 * <li>{ upsert: boolean, multi: boolean, writeConcern: document } for an UPDATE query</li>
	 * <li>{ ordered: boolean, writeConcern: document } argument for an INSERT query</li>
	 * <li>{ justOne: boolean, writeConcern: document } argument for a REMOVE query</li>
	 * </ul>
	 */
	private final Document options;
	private final List<String> unwinds;
	private final List<Document> pipeline;

	public MongoDBQueryDescriptor(String collectionName, Operation operation, List<Document> pipeline) {
		this.collectionName = collectionName;
		this.operation = operation;
		this.criteria = null;
		this.projection = null;
		this.orderBy = null;
		this.options = null;
		this.updateOrInsertOne = null;
		this.updateOrInsertMany = null;
		this.unwinds = null;
		this.pipeline = pipeline == null ? Collections.<Document>emptyList() : pipeline;
		this.distinctFieldName = null;
		this.mapFunction = null;
		this.reduceFunction = null;
	}

	public MongoDBQueryDescriptor(String collectionName, Operation operation, Document criteria, Document projection, Document orderBy, Document options, Document updateOrInsertOne, List<Document> updateOrInsertMany, List<String> unwinds, String distinctFieldName, String mapFunction, String reduceFunction) {
		this.collectionName = collectionName;
		this.operation = operation;
		this.criteria = criteria;
		this.projection = projection;
		this.orderBy = orderBy;
		this.options = options;
		this.updateOrInsertOne = updateOrInsertOne;
		this.updateOrInsertMany = updateOrInsertMany;
		this.unwinds = unwinds;
		this.pipeline = Collections.<Document>emptyList();
		this.distinctFieldName = distinctFieldName;
		this.mapFunction = mapFunction;
		this.reduceFunction = reduceFunction;
	}

	public List<Document> getPipeline() {
		return pipeline;
	}

	/**
	 * The name of the collection to select from.
	 *
	 * @return the collection name
	 */
	public String getCollectionName() {
		return collectionName;
	}

	public Operation getOperation() {
		return operation;
	}

	/**
	 * Criteria describing the records to apply this query to.
	 *
	 * @return the {@link Document} representing the criteria
	 */
	public Document getCriteria() {
		return criteria;
	}

	/**
	 * The fields to be selected, if this query doesn't return all fields of the entity. Passed to the {@code keys}
	 * parameter of the MongoDB find API.
	 *
	 * @return the {@link Document} representing the projection
	 */
	public Document getProjection() {
		return projection;
	}

	/**
	 * Get the order criteria of the result of the query.
	 *
	 * @return the {@link Document} representing the order to apply the results of the query
	 */
	public Document getOrderBy() {
		return orderBy;
	}

	/**
	 * Returns (optional) query options if this is a INSERT, UPDATE or REMOVE query.
	 */
	public Document getOptions() {
		return options;
	}

	/**
	 * Returns the update (new values to apply) in case this is an UPDATE query or values to insert in case this is an
	 * INSERT query.
	 */
	public Document getUpdateOrInsertOne() {
		return updateOrInsertOne;
	}
	public List<Document> getUpdateOrInsertMany() {
		return updateOrInsertMany;
	}

	public List<String> getUnwinds() {
		return unwinds;
	}

	/**
	 * Name of field on which distinct query will be performed
	 */
	public String getDistinctFieldName() {
		return distinctFieldName;
	}

	/**
	 * Returns map function in the MAP_REDUCE operation
	 */
	public String getMapFunction() {
		return mapFunction;
	}

	/**
	 * Returns reduce function in the MAP_REDUCE operation
	 */
	public String getReduceFunction() {
		return reduceFunction;
	}

	@Override
	public String toString() {
		return String.format( "MongoDBQueryDescriptor [collectionName=%s, %s=%s, %s=%s, %s%s]",
			collectionName,
			operation == FINDANDMODIFY ? "document" : operation == INSERT ? "document(s)" : "where", criteria,
			operation == UPDATE ? "update" : operation == INSERT ? "insert" : operation == REMOVE ? "remove" : operation == UPDATEONE ? "updateOne" : "projection", projection,
			operation == UPDATE || operation == INSERT || operation == REMOVE ? "" : "options=", options );
	}
}
