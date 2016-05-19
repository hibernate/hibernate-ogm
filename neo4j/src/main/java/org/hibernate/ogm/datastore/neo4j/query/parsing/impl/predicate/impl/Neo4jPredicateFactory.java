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
import org.hibernate.hql.ast.spi.predicate.ConjunctionPredicate;
import org.hibernate.hql.ast.spi.predicate.DisjunctionPredicate;
import org.hibernate.hql.ast.spi.predicate.InPredicate;
import org.hibernate.hql.ast.spi.predicate.IsNullPredicate;
import org.hibernate.hql.ast.spi.predicate.LikePredicate;
import org.hibernate.hql.ast.spi.predicate.NegationPredicate;
import org.hibernate.hql.ast.spi.predicate.PredicateFactory;
import org.hibernate.hql.ast.spi.predicate.RangePredicate;
import org.hibernate.hql.ast.spi.predicate.RootPredicate;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jAliasResolver;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jPropertyHelper;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jQueryParameter;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.PropertyIdentifier;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Guillaume Smet
 */
public class Neo4jPredicateFactory implements PredicateFactory<StringBuilder> {

	private final Neo4jPropertyHelper propertyHelper;
	private final StringBuilder builder;

	public Neo4jPredicateFactory(Neo4jPropertyHelper propertyHelper) {
		this.propertyHelper = propertyHelper;
		this.builder = new StringBuilder();
	}

	@Override
	public RootPredicate<StringBuilder> getRootPredicate(String entityType) {
		return new Neo4jRootPredicate();
	}

	@Override
	public ComparisonPredicate<StringBuilder> getComparisonPredicate(String entityType, ComparisonPredicate.Type comparisonType,
			List<String> propertyPath, Object value) {
		Object neo4jValue = value instanceof Neo4jQueryParameter ? value : propertyHelper.convertToBackendType( entityType, propertyPath, value );

		PropertyIdentifier columnIdentifier = getPropertyIdentifier( entityType, propertyPath );

		return new Neo4jComparisonPredicate( builder, columnIdentifier.getAlias(), columnIdentifier.getPropertyName(), comparisonType, neo4jValue );
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
		PropertyIdentifier columnIdentifier = getPropertyIdentifier( entityType, propertyPath );
		List<Object> gridTypedElements = new ArrayList<Object>( typedElements.size() );
		for ( Object typedElement : typedElements ) {
			gridTypedElements.add( propertyHelper.convertToBackendType( entityType, propertyPath, typedElement ) );
		}
		return new Neo4jInPredicate( builder, columnIdentifier.getAlias(), columnIdentifier.getPropertyName(), gridTypedElements );
	}

	@Override
	public RangePredicate<StringBuilder> getRangePredicate(String entityType, List<String> propertyPath, Object lowerValue, Object upperValue) {
		PropertyIdentifier columnIdentifier = getPropertyIdentifier( entityType, propertyPath );
		Object neo4jLowerValue = lowerValue instanceof Neo4jQueryParameter ? lowerValue : propertyHelper.convertToBackendType( entityType, propertyPath, lowerValue );
		Object neo4jUpperValue = upperValue instanceof Neo4jQueryParameter ? upperValue : propertyHelper.convertToBackendType( entityType, propertyPath, upperValue );
		return new Neo4jRangePredicate( builder, columnIdentifier.getAlias(), columnIdentifier.getPropertyName(), neo4jLowerValue, neo4jUpperValue );
	}

	@Override
	public NegationPredicate<StringBuilder> getNegationPredicate() {
		return new Neo4jNegationPredicate( builder );
	}

	@Override
	public LikePredicate<StringBuilder> getLikePredicate(String entityType, List<String> propertyPath, String patternValue, Character escapeCharacter) {
		PropertyIdentifier columnIdentifier = getPropertyIdentifier( entityType, propertyPath );
		return new Neo4jLikePredicate( builder, columnIdentifier.getAlias(), columnIdentifier.getPropertyName(), patternValue, escapeCharacter );
	}

	@Override
	public IsNullPredicate<StringBuilder> getIsNullPredicate(String entityType, List<String> propertyPath) {
		PropertyIdentifier columnIdentifier = getPropertyIdentifier( entityType, propertyPath );
		return new Neo4jIsNullPredicate( builder, columnIdentifier.getAlias(), columnIdentifier.getPropertyName() );
	}

	/**
	 * Returns the {@link PropertyIdentifier} corresponding to this property based on information provided by the {@link Neo4jAliasResolver}.
	 *
	 * Note that all the path is required as in the current implementation, the WHERE clause is appended before the OPTIONAL MATCH clauses
	 * so we need all the aliases referenced in the predicates in the MATCH clause. While it's definitely a limitation of the current implementation
	 * it's not really easy to do differently because the OPTIONAL MATCH clauses have to be executed on the filtered nodes and relationships
	 * and we don't have an easy way to know which predicates we should include in the MATCH clause.
	 *
	 * @param entityType the type of the entity
	 * @param propertyPath the path to the property without aliases
	 * @return the corresponding {@link PropertyIdentifier}
	 */
	private PropertyIdentifier getPropertyIdentifier(String entityType, List<String> propertyPath) {
		return propertyHelper.getPropertyIdentifier( entityType, propertyPath, propertyPath.size() );
	}

}
