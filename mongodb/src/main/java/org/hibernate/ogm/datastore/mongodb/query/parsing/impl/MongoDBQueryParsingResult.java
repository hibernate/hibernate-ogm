/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation;
import org.hibernate.ogm.query.spi.QueryParsingResult;

import com.mongodb.DBObject;

/**
 * The result of walking a query parse tree using a {@link MongoDBQueryRendererDelegate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBQueryParsingResult implements QueryParsingResult {

	private final Class<?> entityType;
	private final String collectionName;
	private final DBObject query;
	private final DBObject projection;
	private final DBObject orderBy;

	public MongoDBQueryParsingResult(Class<?> entityType, String collectionName, DBObject query, DBObject projection, DBObject orderBy) {
		this.entityType = entityType;
		this.collectionName = collectionName;
		this.query = query;
		this.projection = projection;
		this.orderBy = orderBy;
	}

	public DBObject getQuery() {
		return query;
	}

	public Class<?> getEntityType() {
		return entityType;
	}

	public DBObject getProjection() {
		return projection;
	}

	public DBObject getOrderBy() {
		return orderBy;
	}

	@Override
	public Object getQueryObject() {
		return new MongoDBQueryDescriptor(
			collectionName,
			Operation.FIND, //so far only SELECT is supported
			query,
			projection,
			orderBy
		);
	}

	@Override
	public List<String> getColumnNames() {
		//TODO Non-scalar case
		return projection != null ? new ArrayList<String>( projection.keySet() ) : Collections.<String>emptyList();
	}

	@Override
	public String toString() {
		return "MongoDBQueryParsingResult [entityType=" + entityType.getSimpleName() + ", query=" + query + ", projection=" + projection + "]";
	}
}
