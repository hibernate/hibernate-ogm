/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

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
import org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl.MongoDBComparisonPredicate;
import org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl.MongoDBConjunctionPredicate;
import org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl.MongoDBDisjunctionPredicate;
import org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl.MongoDBInPredicate;
import org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl.MongoDBIsNullPredicate;
import org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl.MongoDBLikePredicate;
import org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl.MongoDBNegationPredicate;
import org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl.MongoDBRangePredicate;
import org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl.MongoDBRootPredicate;
import org.hibernate.ogm.util.impl.StringHelper;

import com.mongodb.DBObject;

/**
 * Factory for {@link org.hibernate.hql.ast.spi.predicate.Predicate}s creating MongoDB queries in form of
 * {@link DBObject}s.
 *
 * @author Gunnar Morling
 */
public class MongoDBPredicateFactory implements PredicateFactory<DBObject> {

	private final MongoDBPropertyHelper propertyHelper;

	public MongoDBPredicateFactory(MongoDBPropertyHelper propertyHelper) {
		this.propertyHelper = propertyHelper;
	}

	@Override
	public RootPredicate<DBObject> getRootPredicate(String entityType) {
		return new MongoDBRootPredicate();
	}

	@Override
	public ComparisonPredicate<DBObject> getComparisonPredicate(String entityType, Type comparisonType, List<String> propertyPath, Object value) {
		String columnName = propertyHelper.getColumnName( entityType, StringHelper.join( propertyPath, "." ) );
		return new MongoDBComparisonPredicate( columnName, comparisonType, value );
	}

	@Override
	public RangePredicate<DBObject> getRangePredicate(String entityType, List<String> propertyPath, Object lowerValue, Object upperValue) {
		String columnName = propertyHelper.getColumnName( entityType, propertyPath.get( propertyPath.size() - 1 ) );
		return new MongoDBRangePredicate( columnName, lowerValue, upperValue );
	}

	@Override
	public NegationPredicate<DBObject> getNegationPredicate() {
		return new MongoDBNegationPredicate();
	}

	@Override
	public DisjunctionPredicate<DBObject> getDisjunctionPredicate() {
		return new MongoDBDisjunctionPredicate();
	}

	@Override
	public ConjunctionPredicate<DBObject> getConjunctionPredicate() {
		return new MongoDBConjunctionPredicate();
	}

	@Override
	public InPredicate<DBObject> getInPredicate(String entityType, List<String> propertyPath, List<Object> typedElements) {
		String columnName = propertyHelper.getColumnName( entityType, propertyPath.get( propertyPath.size() - 1 ) );
		return new MongoDBInPredicate( columnName, typedElements );
	}

	@Override
	public IsNullPredicate<DBObject> getIsNullPredicate(String entityType, List<String> propertyPath) {
		String columnName = propertyHelper.getColumnName( entityType, propertyPath.get( propertyPath.size() - 1 ) );
		return new MongoDBIsNullPredicate( columnName );
	}

	@Override
	public LikePredicate<DBObject> getLikePredicate(String entityType, List<String> propertyPath, String patternValue, Character escapeCharacter) {
		String columnName = propertyHelper.getColumnName( entityType, propertyPath.get( propertyPath.size() - 1 ) );
		return new MongoDBLikePredicate( columnName, patternValue, escapeCharacter );
	}
}
