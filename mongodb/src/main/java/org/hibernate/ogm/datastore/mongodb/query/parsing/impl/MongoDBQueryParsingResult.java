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

import org.bson.Document;

/**
 * The result of walking a query parse tree using a {@link MongoDBQueryRendererDelegate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBQueryParsingResult implements QueryParsingResult {

	private final Class<?> entityType;
	private final String collectionName;
	private final Document query;
	private final Document projection;
	private final Document orderBy;
	private final List<String> unwinds;

	public MongoDBQueryParsingResult(Class<?> entityType, String collectionName, Document query, Document projection, Document orderBy, List<String> unwinds) {
		this.entityType = entityType;
		this.collectionName = collectionName;
		this.query = query;
		this.projection = projection;
		this.orderBy = orderBy;
		this.unwinds = unwinds;
	}

	public Document getQuery() {
		return query;
	}

	public Class<?> getEntityType() {
		return entityType;
	}

	public Document getProjection() {
		return projection;
	}

	public Document getOrderBy() {
		return orderBy;
	}

	public List<String> getUnwinds() {
		return unwinds;
	}

	@Override
	public Object getQueryObject() {
		return new MongoDBQueryDescriptor(
			collectionName,
			unwinds == null ? Operation.FIND : Operation.AGGREGATE,
			query,
			projection,
			orderBy,
			null,
			null,
			null,
			unwinds,
			null,
			null,
			null
		);
	}

	@Override
	public List<String> getColumnNames() {
		//TODO Non-scalar case
		return projection != null ? new ArrayList<>( projection.keySet() ) : Collections.<String>emptyList();
	}

	@Override
	public String toString() {
		return "MongoDBQueryParsingResult [entityType=" + entityType.getSimpleName() + ", query=" + query + ", projection=" + projection + "]";
	}
}
