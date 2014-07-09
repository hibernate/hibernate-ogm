/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl;

import java.util.Collection;

/**
 * The functions to describe the WHERE...RETURN...ORDER BY... part of a cypher query
 *
 * @author Davide D'Alto
 */
public class WhereClause {

	private final StringBuilder builder;
	private CypherExpression booleanExpression;
	private Collection<OrderByClause> orderByExpression;
	private IdentifierExpression[] projections;

	public WhereClause(StringBuilder builder) {
		this.builder = builder;
	}

	public WhereClause where(CypherExpression booleanExpression) {
		this.booleanExpression = booleanExpression;
		return this;
	}

	public WhereClause orderBy(Collection<OrderByClause> orderByExpression) {
		this.orderByExpression = orderByExpression;
		return this;
	}

	public WhereClause returns(IdentifierExpression... projections) {
		this.projections = projections;
		return this;
	}

	@Override
	public String toString() {
		appendWhereClause();
		appendReturnClause();
		appendOrderByClause();
		return builder.toString();
	}

	private void appendReturnClause() {
		int counter = 1;
		builder.append( " RETURN " );
		for ( IdentifierExpression projection : projections ) {
			projection.asString( builder );
			if ( counter++ < projections.length ) {
				builder.append( ", " );
			}
		}
	}

	private void appendOrderByClause() {
		if ( orderByExpression != null && !orderByExpression.isEmpty() ) {
			builder.append( " ORDER BY " );
			int counter = 1;
			for ( OrderByClause orderBy : orderByExpression ) {
				orderBy.asString( builder );
				if ( counter++ < orderByExpression.size() ) {
					builder.append( ", " );
				}
			}
		}
	}

	private void appendWhereClause() {
		if ( booleanExpression != null ) {
			builder.append( " WHERE " );
			booleanExpression.asString( builder );
		}
	}
}
