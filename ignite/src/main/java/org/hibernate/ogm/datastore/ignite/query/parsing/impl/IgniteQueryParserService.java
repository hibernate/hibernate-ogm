package org.hibernate.ogm.datastore.ignite.query.parsing.impl;

import java.util.Map;

import org.hibernate.engine.jdbc.internal.JdbcServicesImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteHqlQueryParser;
import org.hibernate.ogm.query.spi.BaseQueryParserService;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.query.spi.QueryParsingResult;

/**
 * Ignite-specific implementation of {@link QueryParserService} 
 * 
 * @author Dmitriy Kozlov
 *
 */
public class IgniteQueryParserService extends BaseQueryParserService {

	private static final long serialVersionUID = -2756348489684702772L;
	
	public static final IgniteQueryParserService INSTANCE = new IgniteQueryParserService();
	
	@Override
	public boolean supportsParameters() {
		return false;
	}

	@Override
	public QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString, Map<String, Object> namedParameters) {
		IgniteHqlQueryParser parser = new IgniteHqlQueryParser(queryString, sessionFactory);
		IgniteQueryParsingResult result = new IgniteQueryParsingResult(parser.buildQueryDescriptor(), parser.getColumnNames());
		
		SqlStatementLogger statementLogger = sessionFactory.getServiceRegistry().getService(JdbcServices.class).getSqlStatementLogger();
		statementLogger.logStatement(result.getQueryObject().getSql());
		
		return result;
	}

	@Override
	public QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString) {
		return null;
	}
	
}
