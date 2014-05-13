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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate;
import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate.Type;
import org.hibernate.hql.ast.spi.predicate.ConjunctionPredicate;
import org.hibernate.hql.ast.spi.predicate.DisjunctionPredicate;
import org.hibernate.hql.ast.spi.predicate.InPredicate;
import org.hibernate.hql.ast.spi.predicate.IsNullPredicate;
import org.hibernate.hql.ast.spi.predicate.LikePredicate;
import org.hibernate.hql.ast.spi.predicate.NegationPredicate;
import org.hibernate.hql.ast.spi.predicate.PredicateFactory;
import org.hibernate.hql.ast.spi.predicate.RangePredicate;
import org.hibernate.hql.ast.spi.predicate.RootPredicate;
import org.hibernate.ogm.datastore.neo4j.parser.impl.Neo4jPropertyHelper;
import org.hibernate.ogm.datastore.neo4j.parser.impl.Neo4jQueryResolverDelegate;
import org.neo4j.cypherdsl.expression.BooleanExpression;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jPredicateFactory implements PredicateFactory<BooleanExpression> {

	private final Neo4jPropertyHelper propertyHelper;
	private final Neo4jQueryResolverDelegate resolverDelegate;

	public Neo4jPredicateFactory(Neo4jPropertyHelper propertyHelper, Neo4jQueryResolverDelegate resolverDelegate) {
		this.propertyHelper = propertyHelper;
		this.resolverDelegate = resolverDelegate;
	}

	@Override
	public RootPredicate<BooleanExpression> getRootPredicate(String entityType) {
		return new Neo4jRootPredicate();
	}

	@Override
	public ComparisonPredicate<BooleanExpression> getComparisonPredicate(String entityType, Type comparisonType, List<String> propertyPath, Object value) {
		String columnName = columnName( entityType, propertyPath );
		String alias = alias( entityType );
		Object neo4jValue = propertyHelper.convertToPropertyGridType( entityType, propertyPath, value );
		return new Neo4jComparisonPredicate( alias, columnName, comparisonType, neo4jValue );
	}

	@Override
	public DisjunctionPredicate<BooleanExpression> getDisjunctionPredicate() {
		return new Neo4jDisjunctionPredicate();
	}

	@Override
	public ConjunctionPredicate<BooleanExpression> getConjunctionPredicate() {
		return new Neo4jConjuctionPredicate();
	}

	@Override
	public InPredicate<BooleanExpression> getInPredicate(String entityType, List<String> propertyPath, List<Object> typedElements) {
		String propertyName = columnName( entityType, propertyPath );
		String alias = alias( entityType );
		List<Object> gridTypedElements = new ArrayList<Object>();
		for ( Object typedElement : typedElements ) {
			gridTypedElements.add( propertyHelper.convertToPropertyGridType( entityType, propertyPath, typedElement ) );
		}
		return new Neo4jInPredicate( alias, propertyName, gridTypedElements );
	}

	@Override
	public RangePredicate<BooleanExpression> getRangePredicate(String entityType, List<String> propertyPath, Object lowerValue, Object upperValue) {
		String propertyName = columnName( entityType, propertyPath );
		String alias = alias( entityType );
		Object neo4jLowerValue = propertyHelper.convertToPropertyGridType( entityType, propertyPath, lowerValue );
		Object neo4jUpperValue = propertyHelper.convertToPropertyGridType( entityType, propertyPath, upperValue );
		return new Neo4jRangePredicate( alias, propertyName, neo4jLowerValue, neo4jUpperValue );
	}

	@Override
	public NegationPredicate<BooleanExpression> getNegationPredicate() {
		return new Neo4jNegationPredicate();
	}

	@Override
	public LikePredicate<BooleanExpression> getLikePredicate(String entityType, List<String> propertyPath, String patternValue, Character escapeCharacter) {
		String propertyName = columnName( entityType, propertyPath );
		String alias = alias( entityType );
		return new Neo4jLikePredicate( alias, propertyName, patternValue, escapeCharacter );
	}

	@Override
	public IsNullPredicate<BooleanExpression> getIsNullPredicate(String entityType, List<String> propertyPath) {
		String propertyName = columnName( entityType, propertyPath );
		String alias = alias( entityType );
		return new Neo4jIsNullPredicate( alias, propertyName );
	}

	private String alias(String entityType) {
		return resolverDelegate.findAliasForType( entityType );
	}

	private String columnName(String entityType, List<String> propertyPath) {
		return propertyHelper.getColumnName( entityType, propertyPath.get( propertyPath.size() - 1 ) );
	}

}
