package org.hibernate.ogm.datastore.ignite.query.impl;

import java.util.Map;

import org.hibernate.engine.query.spi.EntityGraphQueryHint;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.classic.QueryTranslatorImpl;
import org.hibernate.hql.spi.FilterTranslator;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.ogm.datastore.ignite.logging.impl.Log;
import org.hibernate.ogm.datastore.ignite.logging.impl.LoggerFactory;
import org.hibernate.ogm.query.impl.FullTextSearchQueryTranslator;
import org.hibernate.ogm.query.spi.QueryParserService;

/**
 * Creates {@link QueryTranslator}s. Depending on whether the underlying datastore supports queries itself, a translator
 * executing native queries or a translator executing Lucene queries via Hibernate Search will be created.
 * 
 * @author Dmitriy Kozlov
 *
 */
public class IgniteQueryTranslatorFactory implements QueryTranslatorFactory {

	private static final long serialVersionUID = -8666799355366665539L;
	private static final Log LOG = LoggerFactory.getLogger();

	public static final IgniteQueryTranslatorFactory INSTANCE = new IgniteQueryTranslatorFactory();
	
	@Override
	public QueryTranslator createQueryTranslator(String queryIdentifier,
			String queryString, Map filters, SessionFactoryImplementor factory,
			EntityGraphQueryHint entityGraphQueryHint) {
		QueryParserService queryParser = factory.getServiceRegistry().getService( QueryParserService.class );
		if (queryParser != null)
			return new IgniteQueryTranslatorImpl(factory, queryParser, queryIdentifier, queryString, filters);
//			return new QueryTranslatorImpl(queryIdentifier, queryString, filters, factory);
		else {
			try {
				return new FullTextSearchQueryTranslator( factory, queryIdentifier, queryString, filters );
			}
			catch (Exception e) {
				throw LOG.cannotLoadLuceneParserBackend( e );
			}
		}
	}

	@Override
	public FilterTranslator createFilterTranslator(String queryIdentifier,
			String queryString, Map filters, SessionFactoryImplementor factory) {
		return null;
	}

}
