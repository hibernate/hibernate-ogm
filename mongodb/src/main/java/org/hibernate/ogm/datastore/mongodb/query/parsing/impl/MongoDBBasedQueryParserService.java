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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.QueryParser;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryImpl;
import org.hibernate.ogm.hibernatecore.impl.OgmSessionImpl;
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

		return new MongoDBQueryImpl( result.getEntityType(), result.getQuery(), result.getProjection(), (OgmSessionImpl) session );
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
