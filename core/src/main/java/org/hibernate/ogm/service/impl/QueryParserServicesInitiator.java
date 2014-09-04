/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.impl.InternalProperties;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Initiator which contributes a {@link QueryParserService} implementation.
 * <p>
 * The implementation can be configured via {@link OgmProperties#QUERY_PARSER_SERVICE}. If no implementation is
 * configured that way, the default implementation as retrieved from the current {@link DatastoreProvider} will be used.
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2012 Red Hat Inc.
 * @author Gunnar Morling
 */
class QueryParserServicesInitiator implements SessionFactoryServiceInitiator<QueryParserService> {

	public static final SessionFactoryServiceInitiator<QueryParserService> INSTANCE = new QueryParserServicesInitiator();

	@Override
	public Class<QueryParserService> getServiceInitiated() {
		return QueryParserService.class;
	}

	@Override
	public QueryParserService initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry) {
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configuration, registry.getService( ClassLoaderService.class ) );

		return propertyReader.property( InternalProperties.QUERY_PARSER_SERVICE, QueryParserService.class )
				.instantiate()
				.withDefaultImplementation( registry.getService( DatastoreProvider.class ).getDefaultQueryParserServiceType() )
				.getValue();
	}

	@Override
	public QueryParserService initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry) {
		throw new UnsupportedOperationException( "Cannot create " + QueryParserService.class.getName() + " service using metadata" );
	}
}
