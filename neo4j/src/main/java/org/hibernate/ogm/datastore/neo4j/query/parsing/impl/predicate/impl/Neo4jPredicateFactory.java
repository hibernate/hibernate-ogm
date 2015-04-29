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
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.AliasResolver;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jPropertyHelper;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jQueryParameter;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jPredicateFactory implements PredicateFactory<StringBuilder> {

	private final Neo4jPropertyHelper propertyHelper;
	private final AliasResolver aliasResolver;
	private final StringBuilder builder;

	public Neo4jPredicateFactory(Neo4jPropertyHelper propertyHelper, AliasResolver embeddedAliasResolver) {
		this.propertyHelper = propertyHelper;
		this.aliasResolver = embeddedAliasResolver;
		this.builder = new StringBuilder();
	}

	@Override
	public RootPredicate<StringBuilder> getRootPredicate(String entityType) {
		return new Neo4jRootPredicate();
	}

	@Override
	public ComparisonPredicate<StringBuilder> getComparisonPredicate(String entityType, Type comparisonType, List<String> propertyPath, Object value) {
		Object neo4jValue = value instanceof Neo4jQueryParameter ? value : propertyHelper.convertToLiteral( entityType, propertyPath, value );
		String columnName = columnName( entityType, propertyPath );
		String alias = alias( entityType, propertyPath );
		return new Neo4jComparisonPredicate( builder, alias, columnName, comparisonType, neo4jValue );
	}

	@Override
	public DisjunctionPredicate<StringBuilder> getDisjunctionPredicate() {
		return new Neo4jDisjunctionPredicate( builder );
	}

	@Override
	public ConjunctionPredicate<StringBuilder> getConjunctionPredicate() {
		return new Neo4jConjunctionPredicate( builder );
	}

	@Override
	public InPredicate<StringBuilder> getInPredicate(String entityType, List<String> propertyPath, List<Object> typedElements) {
		String propertyName = columnName( entityType, propertyPath );
		String alias = alias( entityType, propertyPath );
		List<Object> gridTypedElements = new ArrayList<Object>( typedElements.size() );
		for ( Object typedElement : typedElements ) {
			gridTypedElements.add( propertyHelper.convertToLiteral( entityType, propertyPath, typedElement ) );
		}
		return new Neo4jInPredicate( builder, alias, propertyName, gridTypedElements );
	}

	@Override
	public RangePredicate<StringBuilder> getRangePredicate(String entityType, List<String> propertyPath, Object lowerValue, Object upperValue) {
		String propertyName = columnName( entityType, propertyPath );
		String alias = alias( entityType, propertyPath );
		Object neo4jLowerValue = lowerValue instanceof Neo4jQueryParameter ? lowerValue : propertyHelper.convertToLiteral( entityType, propertyPath, lowerValue );
		Object neo4jUpperValue = upperValue instanceof Neo4jQueryParameter ? upperValue : propertyHelper.convertToLiteral( entityType, propertyPath, upperValue );
		return new Neo4jRangePredicate( builder, alias, propertyName, neo4jLowerValue, neo4jUpperValue );
	}

	@Override
	public NegationPredicate<StringBuilder> getNegationPredicate() {
		return new Neo4jNegationPredicate( builder );
	}

	@Override
	public LikePredicate<StringBuilder> getLikePredicate(String entityType, List<String> propertyPath, String patternValue, Character escapeCharacter) {
		String propertyName = columnName( entityType, propertyPath );
		String alias = alias( entityType, propertyPath );
		return new Neo4jLikePredicate( builder, alias, propertyName, patternValue, escapeCharacter );
	}

	@Override
	public IsNullPredicate<StringBuilder> getIsNullPredicate(String entityType, List<String> propertyPath) {
		String propertyName = columnName( entityType, propertyPath );
		String alias = alias( entityType, propertyPath );
		return new Neo4jIsNullPredicate( builder, alias, propertyName );
	}

	private String columnName(String entityType, List<String> propertyPath) {
		if ( propertyHelper.isEmbeddedProperty( entityType, propertyPath ) ) {
			return propertyHelper.getEmbeddeColumnName( entityType, propertyPath );
		}
		return propertyHelper.getColumnName( entityType, propertyPath );
	}

	private String alias(String entityType, List<String> propertyPath) {
		String targetEntityAlias = aliasResolver.findAliasForType( entityType );
		if ( propertyHelper.isEmbeddedProperty( entityType, propertyPath ) ) {
			return aliasResolver.createAliasForEmbedded( targetEntityAlias, propertyPath, false );
		}
		return targetEntityAlias;
	}
}
