/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.predicate.impl;

import org.hibernate.hql.ast.spi.predicate.RootPredicate;

import org.bson.Document;


/**
 * MongoDB-based implementation of {@link RootPredicate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBRootPredicate extends RootPredicate<Document> {

	@Override
	public Document getQuery() {
		return child == null ? new Document() : child.getQuery();
	}
}
