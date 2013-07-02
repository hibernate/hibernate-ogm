/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.hql.QueryParser;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.lucene.LuceneProcessingChain;
import org.hibernate.hql.lucene.LuceneQueryParsingResult;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * QueryParserService using the ANTLR3-powered LuceneJPQLWalker.
 * Expects the targeted entities and used attributes to be indexed via Hibernate Search,
 * transforming HQL and JPQL in Lucene Queries.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class LuceneBasedQueryParserService implements QueryParserService {

	private final ServiceRegistryImplementor registry;
	private volatile SessionFactoryEntityNamesResolver entityNamesResolver;

	public LuceneBasedQueryParserService(ServiceRegistryImplementor registry, Map configurationValues) {
		this.registry = registry;
		//TODO: make it possible to lookup the SearchFactoryImplementor at initialization time
		//searchFactoryImplementor = lookupSearchFactory( registry );
	}

	@Override
	public Query getParsedQueryExecutor(Session session, String queryString, Map<String, Object> namedParameters) {
		FullTextSession fullTextSession = Search.getFullTextSession( session );

		LuceneQueryParsingResult parsingResult = new QueryParser().parseQuery( queryString, createProcessingChain( session, unwrap( namedParameters ), fullTextSession ) );

		System.out.println( "Lucene Query: " + parsingResult.getQuery() );

		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( parsingResult.getQuery(), parsingResult.getTargetEntity() );
		fullTextQuery.setProjection( parsingResult.getProjections().toArray( new String[parsingResult.getProjections().size()] ) );

		// Following options are mandatory to load matching entities without using a query
		// (chicken and egg problem)
		fullTextQuery.initializeObjectsWith( ObjectLookupMethod.SKIP, DatabaseRetrievalMethod.FIND_BY_ID );
		return fullTextQuery;
	}

	/**
	 * Unwrappes the given named parameters if they are wrapped into {@link TypedValue}s.
	 *
	 * @param namedParameters the original named parameters
	 * @return the unwrapped named parameters
	 */
	private Map<String, Object> unwrap(Map<String, Object> namedParameters) {
		Map<String, Object> unwrapped = new HashMap<String, Object>( namedParameters.size() );

		for ( Entry<String, Object> entry : namedParameters.entrySet() ) {
			Object value = entry.getValue();
			unwrapped.put( entry.getKey(), value instanceof TypedValue ? ( (TypedValue) value ).getValue() : value );
		}

		return unwrapped;
	}

	private LuceneProcessingChain createProcessingChain(Session session, Map<String, Object> namedParameters, FullTextSession fullTextSession) {
		EntityNamesResolver entityNamesResolver = getDefinedEntityNames( session.getSessionFactory() );
		SearchFactoryImplementor searchFactory = (SearchFactoryImplementor) fullTextSession.getSearchFactory();

		return new LuceneProcessingChain( searchFactory, entityNamesResolver, namedParameters );
	}

	private EntityNamesResolver getDefinedEntityNames(SessionFactory sessionFactory) {
		if ( entityNamesResolver == null ) {
			entityNamesResolver = new SessionFactoryEntityNamesResolver( sessionFactory );
		}
		return entityNamesResolver;
	}
}
