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
 */
public class MongoDBQueryDescriptorBuilder {

	private String collection;
	private Operation operation;
	private String criteria;
	private String projection;

	public boolean setCollection(String collection) {
		this.collection = collection.trim();
		return true;
	}

	public boolean setOperation(Operation operation) {
		this.operation = operation;
		return true;
	};

	public boolean setCriteria(String criteria) {
		this.criteria = criteria;
		return true;
	}

	public boolean setProjection(String projection) {
		this.projection = projection;
		return true;
	}

	public MongoDBQueryDescriptor build() {
		return new MongoDBQueryDescriptor( collection, operation, (DBObject) JSON.parse( criteria ), (DBObject) JSON.parse( projection ), null );
	}
}
