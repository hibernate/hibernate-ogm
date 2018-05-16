/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.parsing.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.hql.ast.origin.hql.resolve.path.AggregationPropertyPath;
import org.hibernate.hql.ast.origin.hql.resolve.path.PropertyPath;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.ast.spi.PropertyHelper;
import org.hibernate.hql.ast.spi.SingleEntityQueryBuilder;
import org.hibernate.hql.ast.spi.SingleEntityQueryRendererDelegate;
import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate;

/**
 * SingleEntityQueryRendererDelegate replaces named parameters with the corresponding values when the query is converted,
 * but if the dialect supports queries with parameters it should instead keep the name of the parameters in the query and convert it to the right syntax.
 * We cannot do this at the moment because some methods are private and cannot be overridden {@link SingleEntityQueryRendererDelegate#parameterValue(String)}.
 * Therefore in this class there is a lot of duplicated code that we plan to remove when issue [https://hibernate.atlassian.net/browse/HQLPARSER-79] is solved.
 *
 * @param <Q> Builder class
 * @param <R> Output class
 *
 * @see SingleEntityQueryRendererDelegate
 */
public abstract class KeepNamedParametersQueryRendererDelegate<Q, R> extends SingleEntityQueryRendererDelegate<Q, R> {

	protected PropertyHelper propertyHelper;

	public KeepNamedParametersQueryRendererDelegate(PropertyHelper propertyHelper, EntityNamesResolver entityNames,
			SingleEntityQueryBuilder<Q> builder, Map<String, Object> namedParameters) {
		super( propertyHelper, entityNames, builder, namedParameters );
		this.propertyHelper = propertyHelper;
	}

	@Override
	public void predicateLike(String patternValue, Character escapeCharacter) {
		Object pattern = parameterValue( patternValue );
		List<String> property = resolveAlias( propertyPath );
		if ( status == Status.DEFINING_WHERE ) {
			builder.addLikePredicate( property, (String) pattern, escapeCharacter );
		}
		else if ( status == Status.DEFINING_HAVING ) {
			getHavingBuilder().addLikePredicate( getAggregation(), property, (String) pattern, escapeCharacter );
		}
		else {
			throw new IllegalStateException();
		}
	}

	@Override
	public void predicateBetween(String lower, String upper) {
		Object lowerComparisonValue = parameterValue( lower );
		Object upperComparisonValue = parameterValue( upper );

		List<String> property = resolveAlias( propertyPath );
		if ( status == Status.DEFINING_WHERE ) {
			builder.addRangePredicate( property, lowerComparisonValue, upperComparisonValue );
		}
		else if ( status == Status.DEFINING_HAVING ) {
			getHavingBuilder().addRangePredicate( getAggregation(), property, lowerComparisonValue, upperComparisonValue );
		}
		else {
			throw new IllegalStateException();
		}
	}

	@Override
	public void predicateLess(String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, ComparisonPredicate.Type.LESS );
	}

	@Override
	public void predicateLessOrEqual(String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, ComparisonPredicate.Type.LESS_OR_EQUAL );
	}

	@Override
	public void predicateEquals(final String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, ComparisonPredicate.Type.EQUALS );
	}

	@Override
	public void predicateNotEquals(String comparativePredicate) {
		activateNOT();
		addComparisonPredicate( comparativePredicate, ComparisonPredicate.Type.EQUALS );
		deactivateBoolean();
	}

	@Override
	public void predicateGreaterOrEqual(String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, ComparisonPredicate.Type.GREATER_OR_EQUAL );
	}

	@Override
	public void predicateGreater(String comparativePredicate) {
		addComparisonPredicate( comparativePredicate, ComparisonPredicate.Type.GREATER );
	}

	@Override
	public void predicateIn(List<String> list) {
		List<Object> values = fromNamedQuery( list );
		List<String> property = resolveAlias( propertyPath );
		if ( status == Status.DEFINING_WHERE ) {
			builder.addInPredicate( property, values );
		}
		else if ( status == Status.DEFINING_HAVING ) {
			getHavingBuilder().addInPredicate( getAggregation(), property, values );
		}
		else {
			throw new IllegalStateException();
		}
	}

	private AggregationPropertyPath.Type getAggregation() {
		if ( propertyPath instanceof AggregationPropertyPath ) {
			return ( (AggregationPropertyPath) propertyPath ).getType();
		}
		return null;
	}

	private void addComparisonPredicate(String comparativePredicate, ComparisonPredicate.Type comparisonType) {
		Object comparisonValue = parameterValue( comparativePredicate );
		List<String> property = resolveAlias( propertyPath );
		if ( status == Status.DEFINING_WHERE ) {
			builder.addComparisonPredicate( property, comparisonType, comparisonValue );
		}
		else if ( status == Status.DEFINING_HAVING ) {
			getHavingBuilder().addComparisonPredicate( getAggregation(), property, comparisonType, comparisonValue );
		}
		else {
			throw new IllegalStateException();
		}
	}

	protected List<Object> fromNamedQuery(List<String> list) {
		List<Object> elements = new ArrayList<>( list.size() );

		for ( String string : list ) {
			elements.add( parameterValue( string ) );
		}

		return elements;
	}

	protected Object parameterValue(String comparativePredicate) {
		// It's a named parameter, we need to keep it as it is
		if ( comparativePredicate.startsWith( ":" ) ) {
			return getObjectParameter( comparativePredicate );
		}
		// It's a value given in JP-QL; Convert the literal value
		else {
			List<String> path = new ArrayList<String>();
			path.addAll( propertyPath.getNodeNamesWithoutAlias() );

			PropertyPath fullPath = propertyPath;

			// create the complete path in case it's a join
			while ( fullPath.getFirstNode().isAlias() && aliasToPropertyPath.containsKey( fullPath.getFirstNode().getName() ) ) {
				fullPath = aliasToPropertyPath.get( fullPath.getFirstNode().getName() );
				path.addAll( 0, fullPath.getNodeNamesWithoutAlias() );
			}

			return propertyHelper.convertToPropertyType( targetTypeName, path, comparativePredicate );
		}
	}

	protected Object getObjectParameter(String comparativePredicate) {
		return comparativePredicate;
	}
}
