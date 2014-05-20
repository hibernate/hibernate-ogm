/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.query.parsing.impl;

import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.QueryParser;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.query.impl.DBObjectQuerySpecification;
import org.hibernate.ogm.jpa.impl.NoSQLQueryImpl;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.query.NoSQLQuery;
import org.hibernate.ogm.service.impl.BaseQueryParserService;
import org.hibernate.ogm.service.impl.SessionFactoryEntityNamesResolver;

import com.mongodb.DBObject;

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

		NoSQLQuery query = new DBObjectQuery(
				tableName, result.getQuery(), result.getProjection(),
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

	private static class DBObjectQuery extends NoSQLQueryImpl {

		private final String collectionName;
		private final DBObject query;
		private final DBObject projection;

		public DBObjectQuery(String collectionName, DBObject query, DBObject projection, SessionImplementor session, ParameterMetadata parameterMetadata) {
			super( query.toString(), session, parameterMetadata );

			this.collectionName = collectionName;
			this.query = query;
			this.projection = projection;
		}

		@Override
		protected NativeSQLQuerySpecification generateQuerySpecification(Map namedParams) {
			return new DBObjectQuerySpecification(
					collectionName,
					query,
					projection,
					getQueryReturns().toArray( new NativeSQLQueryReturn[getQueryReturns().size()] ),
					getSynchronizedQuerySpaces()
			);
		}
	}
}
