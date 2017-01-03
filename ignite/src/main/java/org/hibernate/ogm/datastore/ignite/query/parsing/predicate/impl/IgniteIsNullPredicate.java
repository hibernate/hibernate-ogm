/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.parsing.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.IsNullPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;

/**
 * @author Victor Kadachigov
 */
public class IgniteIsNullPredicate extends IsNullPredicate<StringBuilder> implements NegatablePredicate<StringBuilder> {

	private final StringBuilder builder;
	private final String alias;

	public IgniteIsNullPredicate(StringBuilder builder, String alias, String propertyName) {
		super( propertyName );
		this.builder = builder;
		this.alias = alias;
	}

	@Override
	public StringBuilder getQuery() {
		PredicateHelper.identifier( builder, alias, propertyName );
		builder.append( " IS NULL" );
		return builder;
	}

	@Override
	public StringBuilder getNegatedQuery() {
		PredicateHelper.identifier( builder, alias, propertyName );
		builder.append( " IS NOT NULL" );
		return builder;
	}

}
