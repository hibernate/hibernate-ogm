/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.mongodb.query.impl;

import com.mongodb.DBObject;

/**
 * Describes a query to be executed against MongoDB.
 *
 * @author Gunnar Morling
 */
public class MongoDBQueryDescriptor {

	private final String collectionName;
	private final DBObject query;
	private final DBObject projection;

	public MongoDBQueryDescriptor(String collectionName, DBObject query, DBObject projection) {
		this.collectionName = collectionName;
		this.query = query;
		this.projection = projection;
	}

	public MongoDBQueryDescriptor(String collectionName, DBObject query) {
		this( collectionName, query, null );
	}

	/**
	 * The name of the collection to select from.
	 */
	public String getCollectionName() {
		return collectionName;
	}

	/**
	 * The actual query object.
	 */
	public DBObject getQuery() {
		return query;
	}

	/**
	 * The fields to be selected, if this query doesn't return all fields of the entity. Passed to the {@code keys}
	 * parameter of the MongoDB find API.
	 */
	public DBObject getProjection() {
		return projection;
	}
}
