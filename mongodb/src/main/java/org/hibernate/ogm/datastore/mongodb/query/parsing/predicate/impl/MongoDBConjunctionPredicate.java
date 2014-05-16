/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.hql.ast.spi.predicate.ConjunctionPredicate;
import org.hibernate.hql.ast.spi.predicate.NegatablePredicate;
import org.hibernate.hql.ast.spi.predicate.Predicate;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * MongoDB-based implementation of {@link ConjunctionPredicate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBConjunctionPredicate extends ConjunctionPredicate<DBObject> implements NegatablePredicate<DBObject> {

	@Override
	public DBObject getQuery() {
		List<DBObject> elements = new ArrayList<DBObject>();

		for ( Predicate<DBObject> child : children ) {
			elements.add( child.getQuery() );
		}

		return new BasicDBObject("$and", elements);
	}

	@Override
	public DBObject getNegatedQuery() {
		List<DBObject> elements = new ArrayList<DBObject>();

		for ( Predicate<DBObject> child : children ) {
			elements.add( ( (NegatablePredicate<DBObject>) child ).getNegatedQuery() );
		}

		return new BasicDBObject("$or", elements);
	}
}
