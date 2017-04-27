/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.parsing.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.DisjunctionPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.Predicate;

/**
 * @author Victor Kadachigov
 */
public class IgniteDisjunctionPredicate extends DisjunctionPredicate<StringBuilder> implements NegatablePredicate<StringBuilder> {

	private final StringBuilder builder;

	public IgniteDisjunctionPredicate(StringBuilder builder) {
		this.builder = builder;
	}

	@Override
	public StringBuilder getQuery() {
		int counter = 1;
		builder.append( "(" );
		for ( Predicate<StringBuilder> child : children ) {
			child.getQuery();
			builder.append( ")" );
			if ( counter++ < children.size() ) {
				builder.append( " OR (" );
			}
		}
		return builder;
	}

	@Override
	public StringBuilder getNegatedQuery() {
		int counter = 1;
		builder.append( "(" );
		for ( Predicate<StringBuilder> child : children ) {
			( (NegatablePredicate<StringBuilder>) child ).getNegatedQuery();
			builder.append( ")" );
			if ( counter++ < children.size() ) {
				builder.append( " AND (" );
			}
		}
		return builder;
	}

}
