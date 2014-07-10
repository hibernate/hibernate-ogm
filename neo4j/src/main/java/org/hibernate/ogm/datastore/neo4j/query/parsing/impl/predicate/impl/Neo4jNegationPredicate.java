/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.NegationPredicate;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jNegationPredicate extends NegationPredicate<StringBuilder> implements NegatablePredicate<StringBuilder> {

	private final StringBuilder builder;

	public Neo4jNegationPredicate(StringBuilder builder) {
		this.builder = builder;
	}

	@Override
	public StringBuilder getQuery() {
		if ( getChild() instanceof NegatablePredicate ) {
			NegatablePredicate<StringBuilder> negatable = (NegatablePredicate<StringBuilder>) getChild();
			return negatable.getNegatedQuery();
		}
		else {
			builder.append( "NOT (" );
			getChild().getQuery();
			builder.append( ")" );
			return builder;
		}
	}

	@Override
	public StringBuilder getNegatedQuery() {
		return getChild().getQuery();
	}

}
