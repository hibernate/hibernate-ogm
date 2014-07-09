/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.neo4j.cypherdsl.CypherQuery.and;
import static org.neo4j.cypherdsl.CypherQuery.or;

import org.hibernate.hql.ast.spi.predicate.ConjunctionPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.Predicate;
import org.neo4j.cypherdsl.expression.BooleanExpression;


/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 *
 */
public class Neo4jConjuctionPredicate extends ConjunctionPredicate<BooleanExpression> implements NegatablePredicate<BooleanExpression> {

	@Override
	public BooleanExpression getQuery() {
		BooleanExpression[] expressions = new BooleanExpression[children.size()];
		int i = 0;
		for ( Predicate<BooleanExpression> child : children ) {
			expressions[i++] = child.getQuery();
		}
		return and( expressions );
	}

	@Override
	public BooleanExpression getNegatedQuery() {
		BooleanExpression[] expressions = new BooleanExpression[children.size()];
		int i = 0;
		for ( Predicate<BooleanExpression> child : children ) {
			expressions[i++] = ( (NegatablePredicate<BooleanExpression>) child ).getNegatedQuery();
		}
		return or( expressions );
	}

}
