/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.parsing.predicate.impl;

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
import org.hibernate.ogm.datastore.ignite.query.parsing.impl.IgnitePropertyHelper;
import org.hibernate.ogm.datastore.ignite.query.parsing.impl.PropertyIdentifier;

/**
 * @author Victor Kadachigov
 */
public class IgnitePredicateFactory implements PredicateFactory<StringBuilder> {

	private final StringBuilder builder;
	private final IgnitePropertyHelper propertyHelper;

	public IgnitePredicateFactory( IgnitePropertyHelper propertyHelper ) {
		this.builder = new StringBuilder();
		this.propertyHelper = propertyHelper;
	}

	@Override
	public RootPredicate<StringBuilder> getRootPredicate(String entityType) {
		return new IgniteRootPredicate();
	}

	@Override
	public ComparisonPredicate<StringBuilder> getComparisonPredicate(String entityType, Type comparisonType, List<String> propertyPath, Object value) {
		PropertyIdentifier identifier = propertyHelper.getPropertyIdentifier( entityType, propertyPath );
		return new IgniteComparisonPredicate( builder, identifier.getAlias(), identifier.getPropertyName(), comparisonType, value );
	}

	@Override
	public InPredicate<StringBuilder> getInPredicate(String entityType, List<String> propertyPath, List<Object> typedElements) {
		PropertyIdentifier identifier = propertyHelper.getPropertyIdentifier( entityType, propertyPath );
		return new IgniteInPredicate( builder, identifier.getAlias(), identifier.getPropertyName(), typedElements );
	}

	@Override
	public RangePredicate<StringBuilder> getRangePredicate(String entityType, List<String> propertyPath, Object lowerValue, Object upperValue) {
		PropertyIdentifier identifier = propertyHelper.getPropertyIdentifier( entityType, propertyPath );
		return new IgniteRangePredicate( builder, identifier.getAlias(), identifier.getPropertyName(), lowerValue, upperValue );
	}

	@Override
	public NegationPredicate<StringBuilder> getNegationPredicate() {
		return new IgniteNegationPredicate( builder );
	}

	@Override
	public DisjunctionPredicate<StringBuilder> getDisjunctionPredicate() {
		return new IgniteDisjunctionPredicate( builder );
	}

	@Override
	public ConjunctionPredicate<StringBuilder> getConjunctionPredicate() {
		return new IgniteConjunctionPredicate( builder );
	}

	@Override
	public LikePredicate<StringBuilder> getLikePredicate(String entityType, List<String> propertyPath, String patternValue, Character escapeCharacter) {
		PropertyIdentifier identifier = propertyHelper.getPropertyIdentifier( entityType, propertyPath );
		return new IgniteLikePredicate( builder, identifier.getAlias(), identifier.getPropertyName(), patternValue, escapeCharacter );
	}

	@Override
	public IsNullPredicate<StringBuilder> getIsNullPredicate(String entityType, List<String> propertyPath) {
		PropertyIdentifier identifier = propertyHelper.getPropertyIdentifier( entityType, propertyPath );
		return new IgniteIsNullPredicate( builder, identifier.getAlias(), identifier.getPropertyName() );
	}

}
