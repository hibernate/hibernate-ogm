/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.hql.QueryParser;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.hql.lucene.LuceneProcessingChain;
import org.hibernate.hql.lucene.LuceneQueryParsingResult;
import org.hibernate.ogm.hibernatecore.impl.OgmSession;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.Search;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;

/**
 * QueryParserService using the ANTLR3-powered LuceneJPQLWalker.
 * Expects the targeted entities and used attributes to be indexed via Hibernate Search,
 * transforming HQL and JPQL in Lucene Queries.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class LuceneBasedQueryParserService extends BaseQueryParserService {

	private static final Log log = LoggerFactory.make();

	private volatile SessionFactoryEntityNamesResolver entityNamesResolver;

	public LuceneBasedQueryParserService() {
		// TODO: make it possible to lookup the SearchFactoryImplementor at initialization time
		// searchFactoryImplementor = lookupSearchFactory( registry );
	}

	@Override
	public Query getParsedQueryExecutor(OgmSession session, String queryString, Map<String, Object> namedParameters) {
		FullTextSession fullTextSession = Search.getFullTextSession( session );

		LuceneQueryParsingResult parsingResult = new QueryParser().parseQuery( queryString,
				createProcessingChain( session, unwrap( namedParameters ), fullTextSession ) );

		log.createdQuery( queryString, parsingResult.getQuery() );

		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( parsingResult.getQuery(), parsingResult.getTargetEntity() );
		if ( requiresProjections( parsingResult.getProjections() ) ) {
			fullTextQuery.setProjection( parsingResult.getProjections().toArray( new String[parsingResult.getProjections().size()] ) );
		}

		// Following options are mandatory to load matching entities without using a query
		// (chicken and egg problem)
		fullTextQuery.initializeObjectsWith( ObjectLookupMethod.SKIP, DatabaseRetrievalMethod.FIND_BY_ID );
		return fullTextQuery;
	}

	private boolean requiresProjections(List<String> projections) {
		if ( projections.size() == 0 ) {
			return false;
		}
		else if ( projections.size() == 1 && ProjectionConstants.THIS.equals( projections.get( 0 ) ) ) {
			return false;
		}
		else {
			return true;
		}
	}

	private LuceneProcessingChain createProcessingChain(Session session, Map<String, Object> namedParameters, FullTextSession fullTextSession) {
		EntityNamesResolver entityNamesResolver = getDefinedEntityNames( session.getSessionFactory() );
		SearchFactoryImplementor searchFactory = (SearchFactoryImplementor) fullTextSession.getSearchFactory();

		return new LuceneProcessingChain.Builder( searchFactory, entityNamesResolver )
				.namedParameters( namedParameters )
				.buildProcessingChainForClassBasedEntities();
	}

	private EntityNamesResolver getDefinedEntityNames(SessionFactory sessionFactory) {
		if ( entityNamesResolver == null ) {
			entityNamesResolver = new SessionFactoryEntityNamesResolver( sessionFactory );
		}
		return entityNamesResolver;
	}
}
