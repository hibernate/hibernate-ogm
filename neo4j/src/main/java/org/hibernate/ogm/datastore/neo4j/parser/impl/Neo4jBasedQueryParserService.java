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
package org.hibernate.ogm.datastore.neo4j.parser.impl;

import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.QueryParser;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.service.impl.BaseQueryParserService;
import org.hibernate.ogm.service.impl.SessionFactoryEntityNamesResolver;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jBasedQueryParserService extends BaseQueryParserService {

	private static final Log log = LoggerFactory.make();

	private volatile SessionFactoryEntityNamesResolver entityNamesResolver;

	@Override
	public Query getParsedQueryExecutor(OgmSession session, String queryString, Map<String, Object> namedParameters) {
		QueryParser queryParser = new QueryParser();
		Neo4jProcessingChain processingChain = createProcessingChain( session, unwrap( namedParameters ) );
		Neo4jQueryParsingResult result = queryParser.parseQuery( queryString, processingChain );

		log.createdQuery( queryString, result );

		return new Neo4jQuery( result.getEntityType(), result.getQuery().toString(), result.getProjections(), session );
	}

	private Neo4jProcessingChain createProcessingChain(Session session, Map<String, Object> namedParameters) {
		EntityNamesResolver entityNamesResolver = getDefinedEntityNames( session.getSessionFactory() );
		return new Neo4jProcessingChain( (SessionFactoryImplementor) session.getSessionFactory(), entityNamesResolver, namedParameters );
	}

	private EntityNamesResolver getDefinedEntityNames(SessionFactory sessionFactory) {
		if ( entityNamesResolver == null ) {
			entityNamesResolver = new SessionFactoryEntityNamesResolver( sessionFactory );
		}
		return entityNamesResolver;
	}
}
