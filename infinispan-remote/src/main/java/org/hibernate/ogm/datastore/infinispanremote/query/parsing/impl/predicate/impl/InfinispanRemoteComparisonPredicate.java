/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.InfinispanRemoteQueryBuilder;

/**
 * Infinispan remote implementation of {@link ComparisonPredicate}.
 *
 * @author Fabio Massimo Ercoli
 */
public class InfinispanRemoteComparisonPredicate extends ComparisonPredicate<InfinispanRemoteQueryBuilder> implements NegatablePredicate<InfinispanRemoteQueryBuilder> {

	public InfinispanRemoteComparisonPredicate(String propertyName, ComparisonPredicate.Type comparisonType, Object value) {
		super( propertyName, comparisonType, value );
	}

	@Override
	protected InfinispanRemoteQueryBuilder getStrictlyLessQuery() {
		return comparison( " < " );
	}

	@Override
	protected InfinispanRemoteQueryBuilder getLessOrEqualsQuery() {
		return comparison( " <= " );
	}

	@Override
	protected InfinispanRemoteQueryBuilder getEqualsQuery() {
		return comparison( " = " );
	}

	private InfinispanRemoteQueryBuilder getNotEqualsQuery() {
		return comparison( " <> " );
	}

	@Override
	protected InfinispanRemoteQueryBuilder getGreaterOrEqualsQuery() {
		return comparison( " >= " );
	}

	@Override
	protected InfinispanRemoteQueryBuilder getStrictlyGreaterQuery() {
		return comparison( " > " );
	}

	@Override
	public InfinispanRemoteQueryBuilder getNegatedQuery() {
		switch ( type ) {
			case LESS:
				return getGreaterOrEqualsQuery();
			case LESS_OR_EQUAL:
				return getStrictlyGreaterQuery();
			case EQUALS:
				return getNotEqualsQuery();
			case GREATER_OR_EQUAL:
				return getStrictlyLessQuery();
			case GREATER:
				return getLessOrEqualsQuery();
			default:
				throw new UnsupportedOperationException( "Unsupported comparison type: " + type );
		}
	}

	private InfinispanRemoteQueryBuilder comparison(String operator) {
		InfinispanRemoteQueryBuilder builder = new InfinispanRemoteQueryBuilder( propertyName );
		builder.append( operator );
		builder.appendValue( value );

		return builder;
	}
}
