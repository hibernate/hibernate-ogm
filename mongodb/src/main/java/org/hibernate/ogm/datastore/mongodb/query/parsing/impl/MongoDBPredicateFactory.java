/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
		String columnName = propertyHelper.getColumnName( entityType, propertyPath.get( propertyPath.size() - 1 ) );
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
