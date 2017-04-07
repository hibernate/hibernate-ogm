/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.parsing.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.LikePredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;

/**
 * @author Victor Kadachigov
 */
public class IgniteLikePredicate extends LikePredicate<StringBuilder> implements NegatablePredicate<StringBuilder> {

	private final StringBuilder builder;
	private final String alias;

	public IgniteLikePredicate(StringBuilder builder, String alias, String propertyName, String patternValue, Character escapeCharacter) {
		super( propertyName, patternValue, escapeCharacter );
		this.builder = builder;
		this.alias = alias;
	}

	@Override
	public StringBuilder getQuery() {
		PredicateHelper.identifier( builder, alias, propertyName );
		builder.append( " LIKE '" ).append( patternValue ).append( '\'' );
		return builder;
	}

	@Override
	public StringBuilder getNegatedQuery() {
		builder.append( '(' );
		PredicateHelper.identifier( builder, alias, propertyName );
		builder.append( " IS NULL OR " );
		PredicateHelper.identifier( builder, alias, propertyName );
		builder.append( " NOT LIKE '" ).append( patternValue ).append( '\'' );
		builder.append( ')' );
		return builder;
	}
}
