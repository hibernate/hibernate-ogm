/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.NegationPredicate;

import org.bson.Document;


/**
 * MongoDB-based implementation of {@link NegationPredicate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBNegationPredicate extends NegationPredicate<Document> implements NegatablePredicate<Document> {

	@Override
	public Document getQuery() {
		return ( (NegatablePredicate<Document>) getChild() ).getNegatedQuery();
	}

	@Override
	public Document getNegatedQuery() {
		return getChild().getQuery();
	}
}
