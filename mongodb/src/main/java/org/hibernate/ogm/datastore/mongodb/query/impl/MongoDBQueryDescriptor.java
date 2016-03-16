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
		AGGREGATE;
	}

	private final String collectionName;
	private final Operation operation;
	private final DBObject criteria;   // Overloaded to be the 'document' for a FINDANDMODIFY query (which is a kind of criteria),
	                                   //                      document or array of documents to insert for an INSERT query.
	private final DBObject projection; // Overloaded to be the 'update' for an UPDATE query.
	private final DBObject orderBy;    // Overloaded to be the optional { upsert: <boolean>, multi: <boolean>, writeConcern: <document> } argument object for an UPDATE query,
	                                   //                      optional { ordered: <boolean>, writeConcern: <document> } argument for an INSERT query,
	                                   //                      optional { justOne: <boolean>, writeConcern: <document> } argument for a REMOVE query.
	private final List<String> unwinds;

	public MongoDBQueryDescriptor(String collectionName, Operation operation, DBObject criteria, DBObject projection, DBObject orderBy, List<String> unwinds) {
		this.collectionName = collectionName;
		this.operation = operation;
		this.criteria = criteria;
		this.projection = projection;
		this.orderBy = orderBy;
		this.unwinds = unwinds;
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

	public List<String> getUnwinds() {
		return unwinds;
	}

	@Override
	public String toString() {
		return String.format( "MongoDBQueryDescriptor [collectionName=%s, %s=%s, %s=%s, %s%s]",
			collectionName,
			operation == FINDANDMODIFY ? "document" : operation == INSERT ? "document(s)" : "where", criteria,
			operation == UPDATE ? "update" : operation == INSERT ? "insert" : operation == REMOVE ? "remove" : "projection", projection,
			operation == UPDATE || operation == INSERT || operation == REMOVE ? "" : "orderBy=", orderBy);
	}
}
