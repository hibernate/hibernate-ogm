/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.ComparisonPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;

import org.bson.Document;


/**
 * MongoDB-based implementation of {@link ComparisonPredicate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBComparisonPredicate extends ComparisonPredicate<Document> implements NegatablePredicate<Document> {

	public MongoDBComparisonPredicate(String propertyName, ComparisonPredicate.Type comparisonType, Object value) {
		super( propertyName, comparisonType, value );
	}

	@Override
	protected Document getStrictlyLessQuery() {
		return new Document( propertyName, new Document( "$lt", value ) );
	}

	@Override
	protected Document getLessOrEqualsQuery() {
		return new Document( propertyName, new Document( "$lte", value ) );
	}

	@Override
	protected Document getEqualsQuery() {
		return new Document( propertyName, value );
	}

	@Override
	protected Document getGreaterOrEqualsQuery() {
		return new Document( propertyName, new Document( "$gte", value ) );
	}

	@Override
	protected Document getStrictlyGreaterQuery() {
		return new Document( propertyName, new Document( "$gt", value ) );
	}

	@Override
	public Document getNegatedQuery() {
		switch ( type ) {
			case LESS:
				return new Document( propertyName, new Document( "$gte", value ) );
			case LESS_OR_EQUAL:
				return new Document( propertyName, new Document( "$gt", value ) );
			case EQUALS:
				return new Document( propertyName, new Document( "$ne", value ) );
			case GREATER_OR_EQUAL:
				return new Document( propertyName, new Document( "$lt", value ) );
			case GREATER:
				return new Document( propertyName, new Document( "$lte", value ) );
			default:
				throw new UnsupportedOperationException( "Unsupported comparison type: " + type );
		}
	}
}
