/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import org.bson.Document;
import org.hibernate.hql.ast.origin.hql.resolve.path.AggregationPropertyPath;
import org.hibernate.hql.ast.spi.SingleEntityHavingQueryBuilder;
import org.hibernate.hql.ast.spi.SingleEntityQueryRendererDelegate;
import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate;

import java.util.List;

/**
 * Having builder for HQL (MongoBD dialect).
 * This is empty implementation because presense of this class is needed to implement
 * 'group by' clause (OGM-1543)
 * @see SingleEntityQueryRendererDelegate#pushGroupByStrategy()
 *
 * @author Aleksandr Mylnikov
 */
public class MongoDBHavingQueryBuilder implements SingleEntityHavingQueryBuilder<Document> {

	@Override
	public void setEntityType(String entityType) {

	}

	@Override
	public void addComparisonPredicate(AggregationPropertyPath.Type aggregationType, List<String> propertyPath, ComparisonPredicate.Type comparisonType, Object value) {
	}

	@Override
	public void addRangePredicate(AggregationPropertyPath.Type aggregationType, List<String> propertyPath, Object lower, Object upper) {
	}

	@Override
	public void addInPredicate(AggregationPropertyPath.Type aggregationType, List<String> propertyPath, List<Object> elements) {
	}

	@Override
	public void addLikePredicate(AggregationPropertyPath.Type aggregationType, List<String> propertyPath, String patternValue, Character escapeCharacter) {
	}

	@Override
	public void addIsNullPredicate(AggregationPropertyPath.Type aggregationType, List<String> propertyPath) {
	}

	@Override
	public void pushAndPredicate() {
	}

	@Override
	public void pushOrPredicate() {
	}

	@Override
	public void pushNotPredicate() {
	}

	@Override
	public void popBooleanPredicate() {
	}

	@Override
	public Document build() {
		return null;
	}
}
