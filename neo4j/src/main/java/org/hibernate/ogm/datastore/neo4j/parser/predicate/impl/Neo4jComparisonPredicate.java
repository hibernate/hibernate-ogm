/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.neo4j.parser.predicate.impl;

import static org.neo4j.cypherdsl.CypherQuery.identifier;
import static org.neo4j.cypherdsl.CypherQuery.literal;

import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
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
		org.neo4j.cypherdsl.query.Query.checkNull( value, "Value" );
		return new Value( new Operator( identifier( alias ).property( propertyName ), operator ), literal( value ) );
	}

}
