/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.and;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.or;

import org.hibernate.hql.ast.spi.predicate.ConjunctionPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.Predicate;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherExpression;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jConjuctionPredicate extends ConjunctionPredicate<CypherExpression> implements NegatablePredicate<CypherExpression> {

	@Override
	public CypherExpression getQuery() {
		CypherExpression[] expressions = new CypherExpression[children.size()];
		int i = 0;
		for ( Predicate<CypherExpression> child : children ) {
			expressions[i++] = child.getQuery();
		}
		return and( expressions );
	}

	@Override
	public CypherExpression getNegatedQuery() {
		CypherExpression[] expressions = new CypherExpression[children.size()];
		int i = 0;
		for ( Predicate<CypherExpression> child : children) {
			expressions[i++] = ( (NegatablePredicate<CypherExpression>) child ).getNegatedQuery();
		}
		return or( expressions );
	}
}
