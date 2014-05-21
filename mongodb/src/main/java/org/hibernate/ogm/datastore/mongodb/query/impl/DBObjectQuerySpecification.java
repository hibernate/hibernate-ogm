/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.impl;

import java.util.Collection;

import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;

import com.mongodb.DBObject;

/**
 * Describes a query to be executed against MongoDB.
 *
 * @author Gunnar Morling
 */
public class DBObjectQuerySpecification extends NativeSQLQuerySpecification {

	private final String collectionName;
	private final DBObject query;
	private final DBObject projection;
	private final DBObject orderBy;

	public DBObjectQuerySpecification(String collectionName, DBObject query, DBObject projection, DBObject orderBy, NativeSQLQueryReturn[] queryReturns, Collection<String> querySpaces) {
		super( query.toString(), queryReturns, querySpaces );

		this.collectionName = collectionName;
		this.query = query;
		this.projection = projection;
		this.orderBy = orderBy;
	}

	/**
	 * The name of the collection to select from.
	 */
	public String getCollectionName() {
		return collectionName;
	}

	/**
	 * The actual query object.
	 */
	public DBObject getQuery() {
		return query;
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
}
