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

import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;


/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
class QueryParserServicesInitiatior extends OptionalServiceInitiator<QueryParserService> {

	public static final BasicServiceInitiator INSTANCE = new QueryParserServicesInitiatior();

	@Override
	public Class<QueryParserService> getServiceInitiated() {
		return QueryParserService.class;
	}

	@Override
	protected QueryParserService buildServiceInstance(Map configurationValues, ServiceRegistryImplementor registry) {
		// TODO pick a service implementation by configuration options?
		return new LuceneBasedQueryParserService( registry, configurationValues );
	}

	@Override
	protected BasicServiceInitiator<QueryParserService> backupInitiator() {
		return null; //nothing
	}

}
