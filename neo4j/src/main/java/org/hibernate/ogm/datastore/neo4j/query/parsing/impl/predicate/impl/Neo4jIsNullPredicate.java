/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.neo4j.cypherdsl.CypherQuery.identifier;
import static org.neo4j.cypherdsl.CypherQuery.isNotNull;
import static org.neo4j.cypherdsl.CypherQuery.isNull;

import org.hibernate.hql.ast.spi.predicate.IsNullPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.neo4j.cypherdsl.expression.BooleanExpression;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jIsNullPredicate extends IsNullPredicate<BooleanExpression> implements NegatablePredicate<BooleanExpression> {

	private final String alias;

	public Neo4jIsNullPredicate(String alias, String propertyName) {
		super( propertyName );
		this.alias = alias;
	}

	@Override
	public BooleanExpression getQuery() {
		return isNull( identifier( alias ).property( propertyName ) );
	}

	@Override
	public BooleanExpression getNegatedQuery() {
		return isNotNull( identifier( alias ).property( propertyName ) );
	}
}
