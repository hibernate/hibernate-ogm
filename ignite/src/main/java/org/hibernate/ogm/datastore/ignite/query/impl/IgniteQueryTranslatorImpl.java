package org.hibernate.ogm.datastore.ignite.query.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.query.impl.OgmQueryTranslator;
import org.hibernate.ogm.query.spi.QueryParserService;

/**
 * Ignite-specific extension of {@link OgmQueryTranslator}}
 * 
 * @author Dmitriy Kozlov
 *
 */
public class IgniteQueryTranslatorImpl extends OgmQueryTranslator {

	public IgniteQueryTranslatorImpl(SessionFactoryImplementor sessionFactory,
			QueryParserService queryParser, String queryIdentifier,
			String query, Map<?, ?> filters) {
		super(sessionFactory, queryParser, queryIdentifier, query, filters);
	}
	
}
