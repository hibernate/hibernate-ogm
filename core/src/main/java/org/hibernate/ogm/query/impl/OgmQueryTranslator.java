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
import org.hibernate.loader.hql.QueryLoader;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.query.spi.BackendQuery;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.query.spi.QueryParsingResult;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;

/**
 * A {@link QueryTranslator} which converts JP-QL queries into store-dependent native queries.
 *
 * @author Gunnar Morling
 */
public class OgmQueryTranslator extends DelegatingQueryTranslator {

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

	public OgmQueryTranslator(String queryIdentifier, String query, Map<?, ?> filters, SessionFactoryImplementor sessionFactory) {
		super( queryIdentifier, query, filters, sessionFactory );

		this.query = query;
		this.sessionFactory = sessionFactory;
		this.filters = filters;

		ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();
		this.queryParser = serviceRegistry.getService( QueryParserService.class );
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
			QueryParsingResult queryParsingResult = queryParser.parseQuery( sessionFactory, query );

			BackendQuery query = new BackendQuery(
					queryParsingResult.getQueryObject(),
					singleEntityKeyMetadata
			);

			loader = new OgmQueryLoader( delegate, sessionFactory, selectClause, query, queryParsingResult.getColumnNames() );
		}
	}

	@Override
	public List<?> list(SessionImplementor session, QueryParameters queryParameters) throws HibernateException {
		if ( loader != null ) {
			return loader.list( session, queryParameters );
		}
		else {
			QueryParsingResult queryParsingResult = queryParser.parseQuery( sessionFactory, query, getNamedParameterValuesConvertedByGridType( queryParameters ) );

			BackendQuery query = new BackendQuery(
					queryParsingResult.getQueryObject(),
					singleEntityKeyMetadata
			);

			OgmQueryLoader loader = new OgmQueryLoader( delegate, sessionFactory, selectClause, query, queryParsingResult.getColumnNames() );
			return loader.list( session, queryParameters );
		}
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
}
