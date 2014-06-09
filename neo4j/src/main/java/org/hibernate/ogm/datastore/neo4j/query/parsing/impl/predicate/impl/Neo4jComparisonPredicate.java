/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.neo4j.cypherdsl.CypherQuery.identifier;
import static org.neo4j.cypherdsl.CypherQuery.literal;

import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.ogm.util.impl.Contracts;
import org.neo4j.cypherdsl.expression.BooleanExpression;
import org.neo4j.cypherdsl.query.Operator;
import org.neo4j.cypherdsl.query.Value;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jComparisonPredicate extends ComparisonPredicate<BooleanExpression> implements NegatablePredicate<BooleanExpression> {

	private final String alias;

	public Neo4jComparisonPredicate(String alias, String propertyName, ComparisonPredicate.Type comparisonType, Object value) {
		super( propertyName, comparisonType, value );
		this.alias = alias;
	}

	@Override
	protected BooleanExpression getStrictlyLessQuery() {
		return comparator( "<", value );
	}

	@Override
	protected BooleanExpression getLessOrEqualsQuery() {
		return comparator( "<=", value );
	}

	@Override
	protected BooleanExpression getEqualsQuery() {
		return comparator( "=", value );
	}

	private BooleanExpression getNotEqualsQuery() {
		return comparator( "<>", value );
	}

	@Override
	protected BooleanExpression getGreaterOrEqualsQuery() {
		return comparator( ">=", value );
	}

	@Override
	protected BooleanExpression getStrictlyGreaterQuery() {
		return comparator( ">", value );
	}

	@Override
	public BooleanExpression getNegatedQuery() {
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

	private BooleanExpression comparator(String operator, Object value) {
		Contracts.assertNotNull( value, "Value" );
		return new Value( new Operator( identifier( alias ).property( propertyName ), operator ), literal( value ) );
	}

}
