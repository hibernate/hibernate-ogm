/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl;

import java.util.List;

import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;

import org.bson.Document;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
class AggregatePipelineQueryDescriptor extends AbstractQueryDescriptor {
	@Override
	public MongoDBQueryDescriptor build(String collection, MongoDBQueryDescriptor.Operation operation, String criteria, String projection, String orderBy, String options, String updateOrInsert,
										List<Document> pipeline, String distinctFieldName, String collation) {
		return new MongoDBQueryDescriptor( collection, operation, pipeline );
	}
}
