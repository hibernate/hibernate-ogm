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
import org.bson.Document;

/**
 * MongoDB-based implementation of {@link RangePredicate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBRangePredicate extends RangePredicate<Document> implements NegatablePredicate<Document> {

	public MongoDBRangePredicate(String propertyName, Object lower, Object upper) {
		super( propertyName, lower, upper );
	}

	@Override
	public Document getQuery() {
		return new Document(
				"$and",
				Arrays.<Document>asList(
						new Document( propertyName, new Document( "$gte", lower ) ),
						new Document( propertyName, new Document( "$lte", upper ) )
				)
		);
	}

	@Override
	public Document getNegatedQuery() {
		return new Document(
				"$or",
				Arrays.<Document>asList(
						new Document( propertyName, new Document( "$lt", lower ) ),
						new Document( propertyName, new Document( "$gt", upper ) )
				)
		);
	}
}
