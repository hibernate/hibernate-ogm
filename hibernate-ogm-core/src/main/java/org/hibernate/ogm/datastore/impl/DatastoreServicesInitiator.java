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

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.impl.GridDialectFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public final class DatastoreServicesInitiator implements SessionFactoryServiceInitiator<DatastoreServices> {

	public static final DatastoreServicesInitiator INSTANCE = new DatastoreServicesInitiator();

	@Override
	public Class<DatastoreServices> getServiceInitiated() {
		return DatastoreServices.class;
	}

	@Override
	public DatastoreServices initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry) {
		GridDialectFactory dialectFactory = registry.getService( GridDialectFactory.class );
		GridDialect gridDialect = dialectFactory.buildGridDialect( configuration.getProperties(), registry );
		return new DatastoreServicesImpl( gridDialect );
	}

	@Override
	public DatastoreServices initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry) {
		throw new UnsupportedOperationException( "Cannot create " + DatastoreServices.class.getName() + " service using metadata" );
	}

}
