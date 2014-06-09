/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.impl;

import java.util.List;

import org.neo4j.cypherdsl.query.Query;

public class Neo4jQueryParsingResult {

	private final Class<?> entityType;
	private final Query query;
	private final List<String> projections;

	public Neo4jQueryParsingResult(Class<?> entityType, List<String> projections, Query query) {
		this.entityType = entityType;
		this.projections = projections;
		this.query = query;
	}

	public Class<?> getEntityType() {
		return entityType;
	}

	public Query getQuery() {
		return query;
	}

	public List<String> getProjections() {
		return projections;
	}

	@Override
	public String toString() {
		return "Neo4jQueryParsingResult [entityType=" + entityType + ", query=" + query + ", projections=" + projections + "]";
	}

}
