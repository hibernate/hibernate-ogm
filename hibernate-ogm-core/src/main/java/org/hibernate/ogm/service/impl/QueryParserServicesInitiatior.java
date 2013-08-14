/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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

import java.util.Map;

import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Initiator which contributes a {@link QueryParserService} implementation. The implementation can be configured via
 * {@link OgmConfiguration#OGM_QUERY_PARSER_SERVICE}.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 * @author Gunnar Morling
 */
class QueryParserServicesInitiatior extends OptionalServiceInitiator<QueryParserService> {

	public static final BasicServiceInitiator<QueryParserService> INSTANCE = new QueryParserServicesInitiatior();

	private static final Log log = LoggerFactory.make();

	@Override
	public Class<QueryParserService> getServiceInitiated() {
		return QueryParserService.class;
	}

	@Override
	protected QueryParserService buildServiceInstance(Map configurationValues, ServiceRegistryImplementor registry) {
		Object queryParserOption = configurationValues.get( OgmConfiguration.OGM_QUERY_PARSER_SERVICE );

		if ( queryParserOption == null ) {
			return new LuceneBasedQueryParserService( registry, configurationValues );
		}

		Class<?> queryParserServiceClass = null;

		if ( queryParserOption instanceof String ) {
			queryParserServiceClass = registry.getService( ClassLoaderService.class ).classForName( (String) queryParserOption );
		}
		else if ( queryParserOption instanceof Class<?> ) {
			queryParserServiceClass = (Class<?>) queryParserOption;
		}
		else {
			throw log.givenValueForParserServiceConfigurationOptionHasWrongType( queryParserOption );
		}

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

	@Override
	protected BasicServiceInitiator<QueryParserService> backupInitiator() {
		return null; // nothing
	}
}
