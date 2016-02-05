package org.hibernate.ogm.datastore.ignite.query.parsing.impl;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Initiator for the {@link QueryParserService} service for Ignite
 * 
 * @author Dmitriy Kozlov
 *
 */
public class IgniteQueryParserServiceInitiator implements SessionFactoryServiceInitiator<QueryParserService> {

	public static final IgniteQueryParserServiceInitiator INSTANCE = new IgniteQueryParserServiceInitiator();
	
	@Override
	public Class<QueryParserService> getServiceInitiated() {
		return QueryParserService.class;
	}

	@Override
	public QueryParserService initiateService(
			SessionFactoryImplementor sessionFactory,
			SessionFactoryOptions sessionFactoryOptions,
			ServiceRegistryImplementor registry) {
		return IgniteQueryParserService.INSTANCE;
	}

}
