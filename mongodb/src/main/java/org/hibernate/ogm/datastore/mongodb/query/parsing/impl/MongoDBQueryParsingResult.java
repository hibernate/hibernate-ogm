/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import com.mongodb.DBObject;

/**
 * The result of walking a query parse tree using a {@link MongoDBQueryRendererDelegate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBQueryParsingResult {

	private final Class<?> entityType;
	private final DBObject query;
	private final DBObject projection;
	private final DBObject orderBy;

	public MongoDBQueryParsingResult(Class<?> entityType, DBObject query, DBObject projection, DBObject orderBy) {
		this.entityType = entityType;
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
	public String toString() {
		return "MongoDBQueryParsingResult [entityType=" + entityType.getSimpleName() + ", query=" + query + ", projection=" + projection + "]";
	}
}
