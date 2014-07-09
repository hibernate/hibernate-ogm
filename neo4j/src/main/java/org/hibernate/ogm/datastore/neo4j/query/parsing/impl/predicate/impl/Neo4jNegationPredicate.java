/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.not;

import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.NegationPredicate;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherExpression;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jNegationPredicate extends NegationPredicate<CypherExpression> implements NegatablePredicate<CypherExpression> {

	@Override
	public CypherExpression getQuery() {
		if ( getChild() instanceof NegatablePredicate ) {
			NegatablePredicate<CypherExpression> negatable = (NegatablePredicate<CypherExpression>) getChild();
			return negatable.getNegatedQuery();
		}
		return not( getChild().getQuery() );
	}

	@Override
	public CypherExpression getNegatedQuery() {
		return getChild().getQuery();
	}

}
