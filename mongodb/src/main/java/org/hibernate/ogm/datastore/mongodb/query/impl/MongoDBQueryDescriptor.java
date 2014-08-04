/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.impl;

import java.io.Serializable;

import com.mongodb.DBObject;

/**
 * Describes a query to be executed against MongoDB.
 *
 * @author Gunnar Morling
 */
public class MongoDBQueryDescriptor implements Serializable {

	public enum Operation {
		FIND,
		COUNT;
	}

	private final String collectionName;
	private final Operation operation;
	private final DBObject criteria;
	private final DBObject projection;
	private final DBObject orderBy;

	public MongoDBQueryDescriptor(String collectionName, Operation operation, DBObject criteria, DBObject projection, DBObject orderBy) {
		this.collectionName = collectionName;
		this.operation = operation;
		this.criteria = criteria;
		this.projection = projection;
		this.orderBy = orderBy;
	}

	/**
	 * The name of the collection to select from.
	 */
	public String getCollectionName() {
		return collectionName;
	}

	public Operation getOperation() {
		return operation;
	}

	/**
	 * Criteria describing the records to apply this query to.
	 */
	public DBObject getCriteria() {
		return criteria;
	}

	/**
	 * The fields to be selected, if this query doesn't return all fields of the entity. Passed to the {@code keys}
	 * parameter of the MongoDB find API.
	 */
	public DBObject getProjection() {
		return projection;
	}

	public DBObject getOrderBy() {
		return orderBy;
	}



	@Override
	public String toString() {
		return "MongoDBQueryDescriptor [collectionName=" + collectionName + ", where=" + criteria + ", projection=" + projection + ", orderBy=" + orderBy + "]";
	}
}
