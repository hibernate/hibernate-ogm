/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.parsing.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.RangePredicate;

/**
 * @author Victor Kadachigov
 */
public class IgniteRangePredicate extends RangePredicate<StringBuilder> implements NegatablePredicate<StringBuilder> {

	private final String alias;
	private final StringBuilder builder;

	public IgniteRangePredicate(StringBuilder builder, String alias, String propertyName, Object lower, Object upper) {
		super( propertyName, lower, upper );
		this.builder = builder;
		this.alias = alias;
	}

	@Override
	public StringBuilder getQuery() {
		PredicateHelper.identifier( builder, alias, propertyName );
		builder.append( " BETWEEN " );
		PredicateHelper.literal( builder, lower );
		builder.append( " AND " );
		PredicateHelper.literal( builder, upper );
		return builder;
	}

	@Override
	public StringBuilder getNegatedQuery() {
		builder.append( " NOT" );
		getQuery();
		return builder;
	}
}
