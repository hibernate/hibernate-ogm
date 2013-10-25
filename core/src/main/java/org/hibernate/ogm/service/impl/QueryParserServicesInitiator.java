/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012-2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Initiator which contributes a {@link QueryParserService} implementation.
 * <p>
 * The implementation can be configured via {@link OgmConfiguration#OGM_QUERY_PARSER_SERVICE}. If no implementation is
 * configured that way, the default implementation as retrieved from the current {@link DatastoreProvider} will be used.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 * @author Gunnar Morling
 */
class QueryParserServicesInitiator implements SessionFactoryServiceInitiator<QueryParserService> {

	public static final SessionFactoryServiceInitiator<QueryParserService> INSTANCE = new QueryParserServicesInitiator();

	private static final Log log = LoggerFactory.make();

	@Override
	public Class<QueryParserService> getServiceInitiated() {
		return QueryParserService.class;
	}

	@Override
	public QueryParserService initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry) {
		Class<?> queryParserServiceClass = getQueryParserServiceType( configuration, registry );

		if ( !QueryParserService.class.isAssignableFrom( queryParserServiceClass ) ) {
			throw log.givenImplementationClassIsOfWrongType( QueryParserService.class.getName(), queryParserServiceClass.getName() );
		}
		else {
			try {
				return (QueryParserService) queryParserServiceClass.newInstance();
			}
			catch (Exception e) {
				throw log.unableToInstantiateQueryParserService( queryParserServiceClass.getName(), e );
			}
		}
	}

	private Class<?> getQueryParserServiceType(Configuration configuration, ServiceRegistryImplementor registry) {
		String queryParserOption = configuration.getProperty( OgmConfiguration.OGM_QUERY_PARSER_SERVICE );

		if ( queryParserOption != null ) {
			return registry.getService( ClassLoaderService.class ).classForName( queryParserOption );
		}
		else {
			return registry.getService( DatastoreProvider.class ).getDefaultQueryParserServiceType();
		}
	}

	@Override
	public QueryParserService initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry) {
		throw new UnsupportedOperationException( "Cannot create " + QueryParserService.class.getName() + " service using metadata" );
	}
}
