/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.query.parsing.impl;

import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.QueryParser;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.ogm.query.spi.BaseQueryParserService;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.query.spi.QueryParsingResult;
import org.hibernate.ogm.service.impl.SessionFactoryEntityNamesResolver;

/**
 * Ignite-specific implementation of {@link QueryParserService}
 *
 * @author Victor Kadachigov
 */
public class IgniteQueryParserService extends BaseQueryParserService {

	public static final IgniteQueryParserService INSTANCE = new IgniteQueryParserService();

	private volatile SessionFactoryEntityNamesResolver entityNamesResolver;

	@Override
	public boolean supportsParameters() {
		return false;
	}

	@Override
	public QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString, Map<String, Object> namedParameters) {
		QueryParser queryParser = new QueryParser();
		IgniteProcessingChain processingChain = new IgniteProcessingChain( sessionFactory, getDefinedEntityNames( sessionFactory ), namedParameters );
		IgniteQueryParsingResult result = queryParser.parseQuery( queryString, processingChain );

		return result;
	}

	@Override
	public QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString) {
		return null;
	}

	private EntityNamesResolver getDefinedEntityNames(SessionFactory sessionFactory) {
		if ( entityNamesResolver == null ) {
			entityNamesResolver = new SessionFactoryEntityNamesResolver( sessionFactory );
		}
		return entityNamesResolver;
	}
}
