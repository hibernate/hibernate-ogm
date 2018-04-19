package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;

public class InfinispanRemoteComparisonPredicate extends ComparisonPredicate<StringBuilder> implements NegatablePredicate<StringBuilder> {

	public InfinispanRemoteComparisonPredicate(String propertyName, ComparisonPredicate.Type comparisonType, Object value) {
		super( propertyName, comparisonType, value );
	}

	@Override
	protected StringBuilder getStrictlyLessQuery() {
		return comparison( " < " );
	}

	@Override
	protected StringBuilder getLessOrEqualsQuery() {
		return comparison( " <= " );
	}

	@Override
	protected StringBuilder getEqualsQuery() {
		return comparison( " = " );
	}

	private StringBuilder getNotEqualsQuery() {
		return comparison( " <> " );
	}

	@Override
	protected StringBuilder getGreaterOrEqualsQuery() {
		return comparison( " >= " );
	}

	@Override
	protected StringBuilder getStrictlyGreaterQuery() {
		return comparison( " > " );
	}

	@Override
	public StringBuilder getNegatedQuery() {
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

	private StringBuilder comparison(String operator) {
		StringBuilder builder = new StringBuilder(propertyName);
		builder.append( operator );
		builder.append( value );

		return builder;
	}

}
