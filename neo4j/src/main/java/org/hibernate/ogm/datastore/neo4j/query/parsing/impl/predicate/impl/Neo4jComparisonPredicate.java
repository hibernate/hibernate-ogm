/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.compare;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;

import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jComparisonPredicate extends ComparisonPredicate<StringBuilder> implements NegatablePredicate<StringBuilder> {

	private final String alias;
	private final StringBuilder builder;

	public Neo4jComparisonPredicate(StringBuilder builder, String alias, String propertyName, ComparisonPredicate.Type comparisonType, Object value) {
		super( propertyName, comparisonType, value );
		this.builder = builder;
		this.alias = alias;
	}

	@Override
	protected StringBuilder getStrictlyLessQuery() {
		return comparison( "<" );
	}

	@Override
	protected StringBuilder getLessOrEqualsQuery() {
		return comparison( "<=" );
	}

	@Override
	protected StringBuilder getEqualsQuery() {
		return comparison( "=" );
	}

	private StringBuilder getNotEqualsQuery() {
		return comparison( "<>" );
	}

	@Override
	protected StringBuilder getGreaterOrEqualsQuery() {
		return comparison( ">=" );
	}

	@Override
	protected StringBuilder getStrictlyGreaterQuery() {
		return comparison( ">" );
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
		return compare( identifier( builder, alias, propertyName ), operator, value );
	}

}
