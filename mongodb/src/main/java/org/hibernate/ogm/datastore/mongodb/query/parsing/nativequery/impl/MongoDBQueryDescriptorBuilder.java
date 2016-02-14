/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl;

import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * Builder for {@link MongoDBQueryDescriptor}s.
 *
 * @author Gunnar Morling
 * @author Thorsten Möller
 */
public class MongoDBQueryDescriptorBuilder {

	private String collection;
	private Operation operation;
	private String criteria;   // Overloaded to be the 'document' for a FINDANDMODIFY query (which is a kind of criteria),
	                           //                      document or array of documents to insert for an INSERT query.
	private String projection; // Overloaded to be the 'update' for Operation.UPDATE.
	private String orderBy;    // Overloaded to be the optional { upsert: <boolean>, multi: <boolean>, writeConcern: <document> } argument object for Operation.UPDATE,
	                           //                      optional { ordered: <boolean>, writeConcern: <document> } argument object for an INSERT query,
	                           //                      optional { justOne: <boolean>, writeConcern: <document> } argument object for a REMOVE query.

	public boolean setCollection(String collection) {
		this.collection = collection.trim();
		return true;
	}

	public boolean setOperation(Operation operation) {
		this.operation = operation;
		return true;
	}

	public boolean setCriteria(String criteria) {
		this.criteria = criteria;
		return true;
	}

	public boolean setProjection(String projection) {
		this.projection = projection;
		return true;
	}
	
	public boolean setOrderBy(String orderBy) {
		this.orderBy = orderBy;
		return true;
	}

	public MongoDBQueryDescriptor build() {
		return new MongoDBQueryDescriptor(
				collection,
				operation,
				criteria == null ? null : (DBObject) JSON.parse( criteria ),
				projection == null ? null : (DBObject) JSON.parse( projection ),
				orderBy == null ? null: (DBObject) JSON.parse( orderBy ),
				null );
	}
}
