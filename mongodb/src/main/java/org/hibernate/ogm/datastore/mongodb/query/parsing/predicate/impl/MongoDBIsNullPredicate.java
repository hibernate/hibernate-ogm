/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.IsNullPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;

import org.bson.Document;


/**
 * MongoDB-based implementation of {@link IsNullPredicate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBIsNullPredicate extends IsNullPredicate<Document> implements NegatablePredicate<Document> {

	public MongoDBIsNullPredicate(String propertyName) {
		super( propertyName );
	}

	@Override
	public Document getQuery() {
		return new Document( propertyName, new Document( "$exists", false ) );
	}

	@Override
	public Document getNegatedQuery() {
		return new Document( propertyName, new Document( "$exists", true ) );
	}
}
