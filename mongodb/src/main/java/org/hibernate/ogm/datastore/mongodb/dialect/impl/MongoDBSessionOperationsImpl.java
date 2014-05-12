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
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.mongodb.MongoDBSessionOperations;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.query.NoSQLQuery;
import org.hibernate.ogm.query.spi.NativeNoSqlQuery;
import org.hibernate.ogm.util.impl.Contracts;

import com.mongodb.DBObject;

/**
 * Session-level functionality specific to MongoDB.
 *
 * @author Gunnar Morling
 */
public class MongoDBSessionOperationsImpl implements MongoDBSessionOperations {

	private final OgmSession session;

	public MongoDBSessionOperationsImpl(OgmSession session) {
		this.session = session;
	}

	@Override
	public NoSQLQuery createNativeQuery(Class<?> entityType, DBObject query) {
		OgmEntityPersister persister = (OgmEntityPersister) ( ( (SessionImplementor) session )
				.getFactory() )
				.getEntityPersister( entityType.getName() );

		NativeNoSqlQuery<MongoDBQueryDescriptor> mongoDbQuery = new NativeNoSqlQuery<MongoDBQueryDescriptor>(
				new MongoDBQueryDescriptor( persister.getTableName(), query ),
				(SessionImplementor) session,
				new ParameterMetadata( null, null )
		);

		// registering the entity as result; That's save as no projection can be given via this API, so the query
		// only can return the entire entity
		mongoDbQuery.addEntity( entityType );

		return mongoDbQuery;
	}

	@Override
	public NoSQLQuery createNativeQuery(Class<?> entityType, DBObject query, DBObject projection) {
		Contracts.assertParameterNotNull( entityType, "entityType" );
		Contracts.assertParameterNotNull( query, "query" );
		Contracts.assertParameterNotNull( query, "query" );

		OgmEntityPersister persister = (OgmEntityPersister) ( ( (SessionImplementor) session )
				.getFactory() )
				.getEntityPersister( entityType.getName() );

		NativeNoSqlQuery<MongoDBQueryDescriptor> mongoDbQuery = new NativeNoSqlQuery<MongoDBQueryDescriptor>(
				new MongoDBQueryDescriptor( persister.getTableName(), query, projection ),
				(SessionImplementor) session,
				new ParameterMetadata( null, null )
		);

		for ( String field : projection.keySet() ) {
			mongoDbQuery.addScalar( field );
		}

		return mongoDbQuery;
	}
}
