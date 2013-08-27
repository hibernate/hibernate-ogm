/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.mongodb.query.parsing;

import com.mongodb.DBObject;

/**
 * The result of walking a query parse tree using a {@link MongoDBQueryRendererDelegate}.
 *
 * @author Gunnar Morling
 */
public class MongoDBQueryParsingResult {

	private final Class<?> entityType;
	private final DBObject query;
	private final DBObject projection;

	public MongoDBQueryParsingResult(Class<?> entityType, DBObject query, DBObject projection) {
		this.entityType = entityType;
		this.query = query;
		this.projection = projection;
	}

	/**
	 * @return the query
	 */
	public DBObject getQuery() {
		return query;
	}

	/**
	 * @return the entityType
	 */
	public Class<?> getEntityType() {
		return entityType;
	}

	/**
	 * @return the projection
	 */
	public DBObject getProjection() {
		return projection;
	}

	@Override
	public String toString() {
		return "MongoDBQueryParsingResult [entityType=" + entityType.getSimpleName() + ", query=" + query + ", projection=" + projection + "]";
	}
}
