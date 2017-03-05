/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl;

import java.util.List;

import org.hibernate.hql.ast.spi.predicate.InPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;

import org.bson.Document;


/**
 * MongoDB-based implementation of {@link InPredicate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBInPredicate extends InPredicate<Document> implements NegatablePredicate<Document> {

	public MongoDBInPredicate(String propertyName, List<Object> values) {
		super( propertyName, values );
	}

	@Override
	public Document getQuery() {
		return new Document( propertyName, new Document( "$in", values ) );
	}

	@Override
	public Document getNegatedQuery() {
		return new Document( propertyName, new Document( "$nin", values ) );
	}
}
