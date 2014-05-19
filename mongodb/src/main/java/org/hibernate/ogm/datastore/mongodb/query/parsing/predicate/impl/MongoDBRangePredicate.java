/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl;

import java.util.Arrays;

import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.RangePredicate;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * MongoDB-based implementation of {@link RangePredicate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBRangePredicate extends RangePredicate<DBObject> implements NegatablePredicate<DBObject> {

	public MongoDBRangePredicate(String propertyName, Object lower, Object upper) {
		super( propertyName, lower, upper );
	}

	@Override
	public DBObject getQuery() {
		return new BasicDBObject(
				"$and",
				Arrays.<DBObject>asList(
						new BasicDBObject( propertyName, new BasicDBObject( "$gte", lower ) ),
						new BasicDBObject( propertyName, new BasicDBObject( "$lte", upper ) )
				)
		);
	}

	@Override
	public DBObject getNegatedQuery() {
		return new BasicDBObject(
				"$or",
				Arrays.<DBObject>asList(
						new BasicDBObject( propertyName, new BasicDBObject( "$lt", lower ) ),
						new BasicDBObject( propertyName, new BasicDBObject( "$gt", upper ) )
				)
		);
	}
}
