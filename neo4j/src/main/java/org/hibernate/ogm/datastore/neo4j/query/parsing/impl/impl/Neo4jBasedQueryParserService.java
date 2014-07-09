/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.query.parsing.impl.impl;

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
import org.hibernate.ogm.query.NoSQLQuery;
import org.hibernate.ogm.query.impl.NoSQLQueryImpl;
import org.hibernate.ogm.service.impl.BaseQueryParserService;
import org.hibernate.ogm.service.impl.SessionFactoryEntityNamesResolver;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
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
		NoSQLQuery query = nosqlQuery( session, result );
		return query;
	}

	private NoSQLQuery nosqlQuery(OgmSession session, Neo4jQueryParsingResult result) {
		NoSQLQuery query = new NoSQLQueryImpl( result.getQuery(), (SessionImplementor) session, new ParameterMetadata( null, null ) );
		// Register the result types of the query; Currently either a number of scalar values or an entity return
		// are supported only; JP-QL would actually a combination of both, though (see OGM-514)
		if ( hasProjections( result ) ) {
			for ( String field : result.getProjections() ) {
				query.addScalar( field );
			}
		}
		else {
			query.addEntity( result.getEntityType() );
		}
		return query;
	}

	private boolean hasProjections(Neo4jQueryParsingResult result) {
		return result.getProjections() != null && !result.getProjections().isEmpty();
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
