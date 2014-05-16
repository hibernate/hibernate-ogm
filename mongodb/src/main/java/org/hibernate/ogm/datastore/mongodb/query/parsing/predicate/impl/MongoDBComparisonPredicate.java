/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * MongoDB-based implementation of {@link ComparisonPredicate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBComparisonPredicate extends ComparisonPredicate<DBObject> implements NegatablePredicate<DBObject> {

	public MongoDBComparisonPredicate(String propertyName, ComparisonPredicate.Type comparisonType, Object value) {
		super( propertyName, comparisonType, value );
	}

	@Override
	protected DBObject getStrictlyLessQuery() {
		return new BasicDBObject( propertyName, new BasicDBObject( "$lt", value ) );
	}

	@Override
	protected DBObject getLessOrEqualsQuery() {
		return new BasicDBObject( propertyName, new BasicDBObject( "$lte", value ) );
	}

	@Override
	protected DBObject getEqualsQuery() {
		return new BasicDBObject( propertyName, value );
	}

	@Override
	protected DBObject getGreaterOrEqualsQuery() {
		return new BasicDBObject( propertyName, new BasicDBObject( "$gte", value ) );
	}

	@Override
	protected DBObject getStrictlyGreaterQuery() {
		return new BasicDBObject( propertyName, new BasicDBObject( "$gt", value ) );
	}

	@Override
	public DBObject getNegatedQuery() {
		switch ( type ) {
			case LESS:
				return new BasicDBObject( propertyName, new BasicDBObject( "$gte", value ) );
			case LESS_OR_EQUAL:
				return new BasicDBObject( propertyName, new BasicDBObject( "$gt", value ) );
			case EQUALS:
				return new BasicDBObject( propertyName, new BasicDBObject( "$ne", value ) );
			case GREATER_OR_EQUAL:
				return new BasicDBObject( propertyName, new BasicDBObject( "$lt", value ) );
			case GREATER:
				return new BasicDBObject( propertyName, new BasicDBObject( "$lte", value ) );
			default:
				throw new UnsupportedOperationException( "Unsupported comparison type: " + type );
		}
	}
}
