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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.mongodb.DBObject;

/**
 * Describes a query to be executed against MongoDB.
 *
 * @author Gunnar Morling
 * @author Davide D'Alto
 * @author Thorsten Möller
 */
public class MongoDBQueryDescriptor implements Serializable {

	public enum Operation {
		FIND,
		FINDONE,
		FINDANDMODIFY,
		INSERT,
		REMOVE,
		UPDATE,
		COUNT,
		/**
		 * This is used by the query parser when the parsed query requires an aggregation, usually for embedded collections.
		 */
		AGGREGATE,
		/**
		 * This is used for native queries, when the user wants to execute a generic aggregation query.
		 */
		AGGREGATE_PIPELINE,
		DISTINCT;
	}

	private final String collectionName;
	private final Operation operation;
	private final DBObject criteria;   // Overloaded to be the 'document' for a FINDANDMODIFY query (which is a kind of criteria),
	private final DBObject projection;
	private final String fieldName;
	/**
	 * The "update" (new values to apply) in case this is an UPDATE query or values to insert in case this is an INSERT query.
	 */
	private final DBObject updateOrInsert;
	private final DBObject orderBy;

	/**
	 * Optional query options in case this is an UPDATE, INSERT or REMOVE. Will have the following structure:
	 * <ul>
	 * <li>{ upsert: boolean, multi: boolean, writeConcern: document } for an UPDATE query</li>
	 * <li>{ ordered: boolean, writeConcern: document } argument for an INSERT query</li>
	 * <li>{ justOne: boolean, writeConcern: document } argument for a REMOVE query</li>
	 * </ul>
	 */
	private final DBObject options;
	private final List<String> unwinds;
	private final List<DBObject> pipeline;


	public MongoDBQueryDescriptor(String collectionName, Operation operation, DBObject criteria, String fieldName) {
		this.collectionName = collectionName;
		this.operation = operation;
		this.criteria = criteria;
		this.projection = null;
		this.orderBy = null;
		this.options = null;
		this.updateOrInsert = null;
		this.unwinds = null;
		this.pipeline = Collections.<DBObject>emptyList();
		this.fieldName = fieldName;
	}

	public MongoDBQueryDescriptor(String collectionName, Operation operation, List<DBObject> pipeline) {
		this.collectionName = collectionName;
		this.operation = operation;
		this.criteria = null;
		this.projection = null;
		this.orderBy = null;
		this.options = null;
		this.updateOrInsert = null;
		this.unwinds = null;
		this.pipeline = pipeline == null ? Collections.<DBObject>emptyList() : pipeline;
		this.fieldName = "";
	}

	public MongoDBQueryDescriptor(String collectionName, Operation operation, DBObject criteria, DBObject projection, DBObject orderBy, DBObject options, DBObject updateOrInsert, List<String> unwinds) {
		this.collectionName = collectionName;
		this.operation = operation;
		this.criteria = criteria;
		this.projection = projection;
		this.orderBy = orderBy;
		this.options = options;
		this.updateOrInsert = updateOrInsert;
		this.unwinds = unwinds;
		this.pipeline = Collections.<DBObject>emptyList();
		this.fieldName = "";
	}

	public List<DBObject> getPipeline() {
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
	 * @return the {@link DBObject} representing the criteria
	 */
	public DBObject getCriteria() {
		return criteria;
	}

	/**
	 * The fields to be selected, if this query doesn't return all fields of the entity. Passed to the {@code keys}
	 * parameter of the MongoDB find API.
	 *
	 * @return the {@link DBObject} representing the projection
	 */
	public DBObject getProjection() {
		return projection;
	}

	/**
	 * Get the order criteria of the result of the query.
	 *
	 * @return the {@link DBObject} representing the order to apply the results of the query
	 */
	public DBObject getOrderBy() {
		return orderBy;
	}

	/**
	 * Returns (optional) query options if this is a INSERT, UPDATE or REMOVE query.
	 */
	public DBObject getOptions() {
		return options;
	}

	/**
	 * Returns the update (new values to apply) in case this is an UPDATE query or values to insert in case this is an
	 * INSERT query.
	 */
	public DBObject getUpdateOrInsert() {
		return updateOrInsert;
	}

	public List<String> getUnwinds() {
		return unwinds;
	}

	/**
	 * Name of field on which distinct query will be performed
	 */
	public String getFieldName() {
		return fieldName;
	}

	@Override
	public String toString() {
		return String.format( "MongoDBQueryDescriptor [collectionName=%s, %s=%s, %s=%s, %s%s]",
			collectionName,
			operation == FINDANDMODIFY ? "document" : operation == INSERT ? "document(s)" : "where", criteria,
			operation == UPDATE ? "update" : operation == INSERT ? "insert" : operation == REMOVE ? "remove" : "projection", projection,
			operation == UPDATE || operation == INSERT || operation == REMOVE ? "" : "options=", options );
	}
}
