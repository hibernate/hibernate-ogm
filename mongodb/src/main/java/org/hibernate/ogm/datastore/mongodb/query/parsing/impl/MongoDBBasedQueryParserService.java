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
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.QueryParser;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.query.NoSQLQuery;
import org.hibernate.ogm.query.spi.NativeNoSqlQuery;
import org.hibernate.ogm.service.impl.BaseQueryParserService;
import org.hibernate.ogm.service.impl.SessionFactoryEntityNamesResolver;

/**
 * {@link org.hibernate.ogm.service.impl.QueryParserService} implementation which creates MongoDB queries in form of
 * {@link com.mongodb.DBObject}s.
 *
 * @author Gunnar Morling
 */
public class MongoDBBasedQueryParserService extends BaseQueryParserService {

	private static final Log log = LoggerFactory.getLogger();

	private volatile SessionFactoryEntityNamesResolver entityNamesResolver;

	@Override
	public Query getParsedQueryExecutor(OgmSession session, String queryString, Map<String, Object> namedParameters) {
		QueryParser queryParser = new QueryParser();
		MongoDBProcessingChain processingChain = createProcessingChain( session, unwrap( namedParameters ) );

		MongoDBQueryParsingResult result = queryParser.parseQuery( queryString, processingChain );
		log.createdQuery( queryString, result );

		SessionImplementor sessionImplementor = (SessionImplementor) session;

		String tableName = ( (OgmEntityPersister) ( sessionImplementor
				.getFactory() )
				.getEntityPersister( result.getEntityType().getName() ) )
				.getTableName();

		NoSQLQuery query = new NativeNoSqlQuery<MongoDBQueryDescriptor>(
				new MongoDBQueryDescriptor( tableName, result.getQuery(), result.getProjection() ),
				sessionImplementor,
				new ParameterMetadata( null, null )
		);

		// Register the result types of the query; Currently either a number of scalar values or an entity return
		// are supported only; JP-QL would actually a combination of both, though (see OGM-514)
		if ( result.getProjection() != null ) {
			for ( String field : result.getProjection().keySet() ) {
				query.addScalar( field );
			}
		}
		else {
			query.addEntity( result.getEntityType() );
		}

		return query;
	}

	private MongoDBProcessingChain createProcessingChain(Session session, Map<String, Object> namedParameters) {
		EntityNamesResolver entityNamesResolver = getDefinedEntityNames( session.getSessionFactory() );

		return new MongoDBProcessingChain(
				(SessionFactoryImplementor) session.getSessionFactory(),
				entityNamesResolver,
				namedParameters );
	}

	private EntityNamesResolver getDefinedEntityNames(SessionFactory sessionFactory) {
		if ( entityNamesResolver == null ) {
			entityNamesResolver = new SessionFactoryEntityNamesResolver( sessionFactory );
		}
		return entityNamesResolver;
	}
}
