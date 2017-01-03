/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.parsing.predicate.impl;

import java.util.List;

import org.hibernate.hql.ast.spi.predicate.InPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;

/**
 * @author Victor Kadachigov
 */
public class IgniteInPredicate extends InPredicate<StringBuilder> implements NegatablePredicate<StringBuilder> {

	private final StringBuilder builder;
	private final String alias;

	public IgniteInPredicate(StringBuilder builder, String alias, String propertyName, List<Object> values) {
		super( propertyName, values );
		this.builder = builder;
		this.alias = alias;
	}

	@Override
	public StringBuilder getQuery() {
		query( "IN" );
		return builder;
	}

	private void query(String command) {
		PredicateHelper.identifier( builder, alias, propertyName );
		builder.append( ' ' ).append( command ).append( " (" );
		for ( int i = 0; i < values.size(); i++ ) {
			PredicateHelper.literal( builder, values.get( i ) );
			if ( i < values.size() - 1 ) {
				builder.append( ',' );
			}
		}
		builder.append( ')' );
	}

	@Override
	public StringBuilder getNegatedQuery() {
		builder.append( '(' );
		PredicateHelper.identifier( builder, alias, propertyName );
		builder.append( " IS NULL OR " );
		query( "NOT IN" );
		builder.append( ')' );
		return builder;
	}

}
