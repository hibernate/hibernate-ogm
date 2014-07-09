/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.predicate.impl;

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
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.impl.Neo4jPropertyHelper;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.impl.Neo4jQueryResolverDelegate;
import org.neo4j.cypherdsl.expression.BooleanExpression;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
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
		Object neo4jValue = propertyHelper.convertToLiteral( entityType, propertyPath, value );
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
		List<Object> gridTypedElements = new ArrayList<Object>( typedElements.size() );
		for ( Object typedElement : typedElements ) {
			gridTypedElements.add( propertyHelper.convertToLiteral( entityType, propertyPath, typedElement ) );
		}
		return new Neo4jInPredicate( alias, propertyName, gridTypedElements );
	}

	@Override
	public RangePredicate<BooleanExpression> getRangePredicate(String entityType, List<String> propertyPath, Object lowerValue, Object upperValue) {
		String propertyName = columnName( entityType, propertyPath );
		String alias = alias( entityType );
		Object neo4jLowerValue = propertyHelper.convertToLiteral( entityType, propertyPath, lowerValue );
		Object neo4jUpperValue = propertyHelper.convertToLiteral( entityType, propertyPath, upperValue );
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
