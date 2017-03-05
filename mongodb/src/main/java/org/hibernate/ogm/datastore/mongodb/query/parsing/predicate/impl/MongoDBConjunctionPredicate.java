/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.hql.ast.spi.predicate.ConjunctionPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.Predicate;

import org.bson.Document;


/**
 * MongoDB-based implementation of {@link ConjunctionPredicate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBConjunctionPredicate extends ConjunctionPredicate<Document> implements NegatablePredicate<Document> {

	@Override
	public Document getQuery() {
		List<Document> elements = new ArrayList<Document>();

		for ( Predicate<Document> child : children ) {
			elements.add( child.getQuery() );
		}

		return new Document( "$and", elements );
	}

	@Override
	public Document getNegatedQuery() {
		List<Document> elements = new LinkedList<>();

		for ( Predicate<Document> child : children ) {
			elements.add( ( (NegatablePredicate<Document>) child ).getNegatedQuery() );
		}

		return new Document( "$or", elements );
	}
}
