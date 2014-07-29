/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.QueryParser;
import org.hibernate.hql.lucene.LuceneProcessingChain;
import org.hibernate.hql.lucene.LuceneQueryParsingResult;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.ogm.service.impl.SessionFactoryEntityNamesResolver;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.Search;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;

/**
 * A {@link QueryTranslator} which translates JP-QL queries into equivalent Lucene queries and executes those via
 * Hibernate Search.
 *
 * @author Gunnar Morling
 */
public class FullTextSearchQueryTranslator extends LegacyParserBridgeQueryTranslator {

	private final SessionFactoryEntityNamesResolver entityNamesResolver;

	/**
	 * Lucene does not support parameterized queries. As a temporary measure, we therefore cache created queries per set
	 * of parameter values. At one point, this should be replaced by caching the AST after validation but before the
	 * actual Lucene query is created.
	 */
	private final ConcurrentMap<CacheKey, LuceneQueryParsingResult> luceneQueryCache;

	public FullTextSearchQueryTranslator(SessionFactoryImplementor sessionFactory, String queryIdentifier, String query, Map<?, ?> filters) {
		super( sessionFactory, queryIdentifier, query, filters );
		entityNamesResolver = new SessionFactoryEntityNamesResolver( sessionFactory );

		luceneQueryCache = new BoundedConcurrentHashMap<CacheKey, LuceneQueryParsingResult>(
				100,
				20,
				BoundedConcurrentHashMap.Eviction.LIRS
		);
	}

	@Override
	public void doCompile(Map replacements, boolean shallow) throws QueryException, MappingException {
	}

	@Override
	public List<?> list(SessionImplementor session, QueryParameters queryParameters) throws HibernateException {
		FullTextSession fullTextSession = Search.getFullTextSession( (Session) session );

		LuceneQueryParsingResult parsingResult = getLuceneQuery( queryParameters, fullTextSession );

		FullTextQuery fullTextQuery = fullTextSession.createFullTextQuery( parsingResult.getQuery(), parsingResult.getTargetEntity() );

		if ( requiresProjections( parsingResult.getProjections() ) ) {
			fullTextQuery.setProjection( parsingResult.getProjections().toArray( new String[parsingResult.getProjections().size()] ) );
		}

		fullTextQuery.setSort( parsingResult.getSort() );

		// Following options are mandatory to load matching entities without using a query
		// (chicken and egg problem)
		fullTextQuery.initializeObjectsWith( ObjectLookupMethod.SKIP, DatabaseRetrievalMethod.FIND_BY_ID );

		if ( queryParameters.getRowSelection().getFirstRow() != null ) {
			fullTextQuery.setFirstResult( queryParameters.getRowSelection().getFirstRow() );
		}
		if ( queryParameters.getRowSelection().getMaxRows() != null ) {
			fullTextQuery.setMaxResults( queryParameters.getRowSelection().getMaxRows() );
		}

		return fullTextQuery.list();
	}

	private LuceneQueryParsingResult getLuceneQuery(QueryParameters queryParameters, FullTextSession fullTextSession) {
		CacheKey cacheKey = new CacheKey( queryParameters.getNamedParameters() );
		LuceneQueryParsingResult parsingResult = luceneQueryCache.get( cacheKey );

		if ( parsingResult == null ) {
			parsingResult = new QueryParser().parseQuery(
					getQueryString(),
					createProcessingChain( getNamedParameterValues( queryParameters ), fullTextSession )
			);

			LuceneQueryParsingResult cached = luceneQueryCache.putIfAbsent( cacheKey, parsingResult );
			if ( cached != null ) {
				parsingResult = cached;
			}
		}

		return parsingResult;
	}

	@Override
	public Iterator<?> iterate(QueryParameters queryParameters, EventSource session) throws HibernateException {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public ScrollableResults scroll(QueryParameters queryParameters, SessionImplementor session) throws HibernateException {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}

	@Override
	public int executeUpdate(QueryParameters queryParameters, SessionImplementor session) throws HibernateException {
		throw new UnsupportedOperationException( "Not yet implemented" );
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

	private LuceneProcessingChain createProcessingChain(Map<String, Object> namedParameters, FullTextSession fullTextSession) {
		SearchFactoryImplementor searchFactory = (SearchFactoryImplementor) fullTextSession.getSearchFactory();

		return new LuceneProcessingChain.Builder( searchFactory, entityNamesResolver )
				.namedParameters( namedParameters )
				.buildProcessingChainForClassBasedEntities();
	}

	private Map<String, Object> getNamedParameterValues(QueryParameters queryParameters) {
		Map<String, Object> parameterValues = new HashMap<String, Object>( queryParameters.getNamedParameters().size() );

		for ( Entry<String, TypedValue> parameter : queryParameters.getNamedParameters().entrySet() ) {
			parameterValues.put( parameter.getKey(), parameter.getValue().getValue() );
		}

		return parameterValues;
	}

	private static class CacheKey {

		private final Map<String, TypedValue> parameters;
		private final int hashCode;

		public CacheKey(Map<String, TypedValue> parameters) {
			this.parameters = Collections.unmodifiableMap( parameters );
			this.hashCode = parameters.hashCode();
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			CacheKey other = (CacheKey) obj;
			if ( parameters == null ) {
				if ( other.parameters != null ) {
					return false;
				}
			}
			else if ( !parameters.equals( other.parameters ) ) {
				return false;
			}
			return true;
		}
	}
}
