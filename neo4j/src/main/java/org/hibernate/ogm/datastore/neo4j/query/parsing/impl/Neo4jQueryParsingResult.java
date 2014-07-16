/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl;

import java.util.List;

import org.hibernate.ogm.query.spi.QueryParsingResult;

public class Neo4jQueryParsingResult implements QueryParsingResult {

	private final Class<?> entityType;
	private final String query;
	private final List<String> projections;

	public Neo4jQueryParsingResult(Class<?> entityType, List<String> projections, String query) {
		this.entityType = entityType;
		this.projections = projections;
		this.query = query;
	}

	public Class<?> getEntityType() {
		return entityType;
	}

	public List<String> getProjections() {
		return projections;
	}

	@Override
	public String toString() {
		return "Neo4jQueryParsingResult [entityType=" + entityType + ", query=" + query + ", projections=" + projections + "]";
	}

	@Override
	public Object getQueryObject() {
		return query;
	}

	@Override
	public List<String> getColumnNames() {
		return projections;
	}
}
