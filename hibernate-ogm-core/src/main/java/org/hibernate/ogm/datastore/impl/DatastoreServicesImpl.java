/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.impl;

import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.impl.GridDialectFactoryImpl;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.Map;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public final class DatastoreServicesImpl implements DatastoreServices, ServiceRegistryAwareService, Configurable {

	private ServiceRegistryImplementor serviceRegistry;
	private GridDialect gridDialect;

	@Override
	public GridDialect getGridDialect() {
		return gridDialect;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void configure(Map configurationValues) {
		GridDialectFactoryImpl dialectFactory = serviceRegistry.getService( GridDialectFactoryImpl.class );
		DatastoreProvider datastoreProvider = serviceRegistry.getService( DatastoreProvider.class );
		this.gridDialect = dialectFactory.buildGridDialect( configurationValues, serviceRegistry );
		this.serviceRegistry = null;
	}
}
