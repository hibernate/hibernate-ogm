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
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.event.spi.EventSource;
import org.hibernate.hql.internal.ast.HqlParser;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.QueryTranslatorImpl.JavaConstantConverter;
import org.hibernate.hql.internal.ast.tree.SelectClause;
import org.hibernate.hql.internal.ast.util.NodeTraverser;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.internal.util.collections.BoundedConcurrentHashMap;
import org.hibernate.loader.hql.QueryLoader;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.query.spi.QueryParsingResult;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

/**
 * A {@link QueryTranslator} which converts JP-QL queries into store-dependent native queries, e.g. Cypher queries for
 * Neo4j or {@code DBObject}-based queries for MongoDB.
 * <p>
 * Query conversion is done by invoking the dialect's query parser service. Results are loaded through OgmQueryLoader.
 * Depending on whether a store supports parameterized queries (Neo4j does, MongoDB doesn't) we either use one and the
 * same loader for a query executed several times with different parameter values or we create a new loader for each set
 * of parameter values.
 *
 * @author Gunnar Morling
 */
public class OgmQueryTranslator extends LegacyParserBridgeQueryTranslator {

	private static final Log log = LoggerFactory.make();

	private final String query;
	private final SessionFactoryImplementor sessionFactory;
	private final Map<?, ?> filters;

	private final QueryParserService queryParser;

	/**
	 * The query loader in case the dialect supports parameterized queries; We can re-execute it then with different
	 * parameter values.
	 */
	private OgmQueryLoader loader;

	/**
	 * Needed to create query loaders. This won't be required anymore once {@link OgmQueryLoader} doesn't depend that
	 * much on {@link QueryLoader}.
	 */
	private SelectClause selectClause;

	private EntityKeyMetadata singleEntityKeyMetadata;

	/**
	 * Not all stores support parameterized queries. As a temporary measure, we therefore cache created queries per set
	 * of parameter values. At one point, this should be replaced by caching the AST after validation but before the
	 * actual Lucene query is created.
	 */
	private final ConcurrentMap<CacheKey, QueryParsingResult> queryCache;

	public OgmQueryTranslator(SessionFactoryImplementor sessionFactory, QueryParserService queryParser, String queryIdentifier, String query, Map<?, ?> filters) {
		super( sessionFactory, queryIdentifier, query, filters );

		this.queryParser = queryParser;
		this.query = query;
		this.sessionFactory = sessionFactory;
		this.filters = filters;

		queryCache = new BoundedConcurrentHashMap<CacheKey, QueryParsingResult>(
				100,
				20,
				BoundedConcurrentHashMap.Eviction.LIRS
		);
	}

	@Override
	protected void doCompile(Map replacements, boolean shallow) throws QueryException, MappingException {
		try {
			// Unfortunately, we cannot obtain the select clause from the delegate, so we need to parse it again
			selectClause = getSelectClause( replacements, null );
			Type[] queryReturnTypes = selectClause.getQueryReturnTypes();
			this.singleEntityKeyMetadata = getSingleEntityKeyMetadataOrNull( queryReturnTypes );
		}
		catch ( Exception qse ) {
			throw log.querySyntaxException( qse, query );
		}

		if ( queryParser.supportsParameters() ) {
			loader = getLoader( null );
		}
	}

	@Override
	public List<?> list(SessionImplementor session, QueryParameters queryParameters) throws HibernateException {
		OgmQueryLoader loaderToUse = loader != null ? loader : getLoader( queryParameters );
		return loaderToUse.list( session, queryParameters );
	}

	private <T> OgmQueryLoader getLoader(QueryParameters queryParameters) {
		QueryParsingResult queryParsingResult = queryParameters != null ?
				getQuery( queryParameters ) :
				queryParser.parseQuery( sessionFactory, query );

		BackendQuery<T> query = new BackendQuery<T>( (T) queryParsingResult.getQueryObject(), singleEntityKeyMetadata );

		return new OgmQueryLoader( delegate, sessionFactory, selectClause, query, queryParsingResult.getColumnNames() );
	}

	/**
	 * Returns the {@link EntityKeyMetadata} of the entity type selected by this query.
	 * @param queryReturnTypes
	 *
	 * @return the {@link EntityKeyMetadata} of the entity type selected by this query or {@code null} in case this
	 * query does not select exactly one entity type (e.g. in case of scalar values or joins (if supported in future revisions)).
	 */
	private EntityKeyMetadata getSingleEntityKeyMetadataOrNull(Type[] queryReturnTypes) {
		EntityKeyMetadata metadata = null;

		for ( Type queryReturn : queryReturnTypes ) {
			if ( queryReturn instanceof EntityType ) {
				if ( metadata != null ) {
					return null;
				}
				EntityType rootReturn = (EntityType) queryReturn;
				OgmEntityPersister persister = (OgmEntityPersister) sessionFactory.getEntityPersister( rootReturn.getName() );
				metadata = new EntityKeyMetadata( persister.getTableName(), persister.getRootTableIdentifierColumnNames() );
			}
		}

		return metadata;
	}

	private QueryParsingResult getQuery(QueryParameters queryParameters) {
		CacheKey cacheKey = new CacheKey( queryParameters.getNamedParameters() );
		QueryParsingResult parsingResult = queryCache.get( cacheKey );

		if ( parsingResult == null ) {
			parsingResult = queryParser.parseQuery(
					sessionFactory,
					query,
					getNamedParameterValuesConvertedByGridType( queryParameters )
			);

			QueryParsingResult cached = queryCache.putIfAbsent( cacheKey, parsingResult );
			if ( cached != null ) {
				parsingResult = cached;
			}
		}

		return parsingResult;
	}

	/**
	 * Returns a map with the named parameter values from the given parameters object, converted by the {@link GridType}
	 * corresponding to each parameter type.
	 */
	private Map<String, Object> getNamedParameterValuesConvertedByGridType(QueryParameters queryParameters) {
		Map<String, Object> parameterValues = new HashMap<String, Object>( queryParameters.getNamedParameters().size() );
		for ( Entry<String, TypedValue> parameter : queryParameters.getNamedParameters().entrySet() ) {
			parameterValues.put( parameter.getKey(), parameter.getValue().getValue() );
		}

		return parameterValues;
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

	private SelectClause getSelectClause(Map<?, ?> replacements, String collectionRole) throws Exception {
		if ( replacements == null ) {
			replacements = Collections.emptyMap();
		}

		// PHASE 1 : Parse the HQL into an AST.
		final HqlParser parser = parse( true );

		// PHASE 2 : Analyze the HQL AST, and produce an SQL AST.
		final HqlSqlWalker w = analyze( parser, replacements, collectionRole );

		return w.getSelectClause();
	}

	private HqlSqlWalker analyze(HqlParser parser, Map<?, ?> tokenReplacements, String collectionRole) throws QueryException, RecognitionException {
		final HqlSqlWalker w = new HqlSqlWalker( delegate, sessionFactory, parser, tokenReplacements, collectionRole ) {
			@Override
			public Map getEnabledFilters() {
				return filters;
			}
		};
		final AST hqlAst = parser.getAST();

		// Transform the tree.
		w.statement( hqlAst );

		w.getParseErrorHandler().throwQueryException();

		return w;
	}

	private HqlParser parse(boolean filter) throws TokenStreamException, RecognitionException {
		// Parse the query string into an HQL AST.
		final HqlParser parser = HqlParser.getInstance( query );
		parser.setFilter( filter );

		parser.statement();

		final AST hqlAst = parser.getAST();

		final NodeTraverser walker = new NodeTraverser( new JavaConstantConverter() );
		walker.traverseDepthFirst( hqlAst );

		parser.getParseErrorHandler().throwQueryException();
		return parser;
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
