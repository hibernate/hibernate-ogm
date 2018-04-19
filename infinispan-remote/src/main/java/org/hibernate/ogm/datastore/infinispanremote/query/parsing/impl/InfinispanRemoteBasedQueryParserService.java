package org.hibernate.ogm.datastore.infinispanremote.query.parsing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.QueryParser;
import org.hibernate.hql.ast.spi.EntityNamesResolver;
import org.hibernate.ogm.query.spi.BaseQueryParserService;
import org.hibernate.ogm.query.spi.QueryParsingResult;
import org.hibernate.ogm.service.impl.SessionFactoryEntityNamesResolver;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

public class InfinispanRemoteBasedQueryParserService extends BaseQueryParserService {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private volatile SessionFactoryEntityNamesResolver entityNamesResolver;

	@Override
	public QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString, Map<String, Object> namedParameters) {
		throw new UnsupportedOperationException( "The Infinispan Remote query parser supports parameterized queries" );
	}

	@Override
	public QueryParsingResult parseQuery(SessionFactoryImplementor sessionFactory, String queryString) {
		QueryParser queryParser = new QueryParser();
		InfinispanRemoteProcessingChain processingChain = createProcessingChain( sessionFactory );
		InfinispanRemoteQueryParsingResult result = queryParser.parseQuery( queryString, processingChain );

		log.createdQuery( queryString, result );

		return result;
	}

	@Override
	public boolean supportsParameters() {
		return true;
	}

	private InfinispanRemoteProcessingChain createProcessingChain(SessionFactoryImplementor sessionFactory) {
		EntityNamesResolver entityNamesResolver = getDefinedEntityNames( sessionFactory );
		return new InfinispanRemoteProcessingChain( sessionFactory, entityNamesResolver, Collections.emptyMap() );
	}

	private EntityNamesResolver getDefinedEntityNames(SessionFactory sessionFactory) {
		if ( entityNamesResolver == null ) {
			entityNamesResolver = new SessionFactoryEntityNamesResolver( sessionFactory );
		}
		return entityNamesResolver;
	}
}
