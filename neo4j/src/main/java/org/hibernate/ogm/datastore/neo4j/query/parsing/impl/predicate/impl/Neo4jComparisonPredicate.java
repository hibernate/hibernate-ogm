/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.identifier;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.literal;

import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.ComparisonExpression;
import org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherExpression;
import org.hibernate.ogm.util.impl.Contracts;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jComparisonPredicate extends ComparisonPredicate<CypherExpression> implements NegatablePredicate<CypherExpression> {

	private final String alias;

	public Neo4jComparisonPredicate(String alias, String propertyName, ComparisonPredicate.Type comparisonType, Object value) {
		super( propertyName, comparisonType, value );
		this.alias = alias;
	}

	@Override
	protected CypherExpression getStrictlyLessQuery() {
		return comparator( "<" );
	}

	@Override
	protected CypherExpression getLessOrEqualsQuery() {
		return comparator( "<=" );
	}

	@Override
	protected CypherExpression getEqualsQuery() {
		return comparator( "=" );
	}

	private CypherExpression getNotEqualsQuery() {
		return comparator( "<>" );
	}

	@Override
	protected CypherExpression getGreaterOrEqualsQuery() {
		return comparator( ">=" );
	}

	@Override
	protected CypherExpression getStrictlyGreaterQuery() {
		return comparator( ">" );
	}

	@Override
	public CypherExpression getNegatedQuery() {
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

	private CypherExpression comparator(String operator) {
		Contracts.assertNotNull( value, "Value" );
		return new ComparisonExpression( identifier( alias ).property( propertyName ), operator, literal( value ) );
	}

}
