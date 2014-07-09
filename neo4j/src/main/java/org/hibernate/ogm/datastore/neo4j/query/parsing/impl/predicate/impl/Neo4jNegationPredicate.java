/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.neo4j.cypherdsl.CypherQuery.not;

import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.NegationPredicate;
import org.neo4j.cypherdsl.expression.BooleanExpression;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jNegationPredicate extends NegationPredicate<BooleanExpression> implements NegatablePredicate<BooleanExpression> {

	@Override
	public BooleanExpression getQuery() {
		if ( getChild() instanceof NegatablePredicate ) {
			NegatablePredicate<BooleanExpression> negatable = (NegatablePredicate<BooleanExpression>) getChild();
			return negatable.getNegatedQuery();
		}
		return not( getChild().getQuery() );
	}

	@Override
	public BooleanExpression getNegatedQuery() {
		return getChild().getQuery();
	}

}
