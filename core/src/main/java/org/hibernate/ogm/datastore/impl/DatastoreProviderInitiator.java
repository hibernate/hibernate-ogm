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

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.util.impl.ConfigurationPropertyReader;
import org.hibernate.ogm.util.impl.ConfigurationPropertyReader.ShortNameResolver;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Loads the appropriate {@link DatastoreProvider}. Driven by the {@link #DATASTORE_PROVIDER}
 * property which can receive
 * <ul>
 *   <li>a {@code DatastoreProvider} instance</li>
 *   <li>a {@code DatastoreProvider} class</li>
 *   <li>a string representing the {@code DatastoreProvider} class</li>
 *   <li>a string representing one of the datastore provider shortcuts</li>
 * </ul>
 *
 * If the property is not set, Infinispan is used by default.
 *
 * This is a {@link SessionFactoryServiceInitiator} because a {@code DatastoreProvider} can be a
 * {@link org.hibernate.ogm.datastore.StartStoppable} service - the {@link org.hibernate.service.spi.SessionFactoryServiceRegistry}
 * calls {@code StartStoppable} and passes the {@link org.hibernate.SessionFactory}
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Davide D'Alto <davide@hibernate.org>
 */
public final class DatastoreProviderInitiator implements SessionFactoryServiceInitiator<DatastoreProvider> {

	public static final String DATASTORE_PROVIDER = "hibernate.ogm.datastore.provider";
	public static final DatastoreProviderInitiator INSTANCE = new DatastoreProviderInitiator();

	private static final Log log = LoggerFactory.make();
	private static final String DEFAULT_DATASTORE_PROVIDER = "infinispan";

	@Override
	public Class<DatastoreProvider> getServiceInitiated() {
		return DatastoreProvider.class;
	}

	@Override
	public DatastoreProvider initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry) {
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configuration, registry.getService( ClassLoaderService.class ) );

		DatastoreProvider datastoreProvider = propertyReader.getValue(
				DATASTORE_PROVIDER,
				DatastoreProvider.class,
				DEFAULT_DATASTORE_PROVIDER,
				DatastoreProviderShortNameResolver.INSTANCE
		);

		log.useDatastoreProvider( datastoreProvider.getClass().getName() );
		return datastoreProvider;
	}

	@Override
	public DatastoreProvider initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry) {
		throw new UnsupportedOperationException( "Cannot create " + DatastoreProvider.class.getName() + " service using metadata" );
	}

	private static class DatastoreProviderShortNameResolver implements ShortNameResolver {

		private static final DatastoreProviderShortNameResolver INSTANCE = new DatastoreProviderShortNameResolver();

		@Override
		public boolean isShortName(String name) {
			return AvailableDatastoreProvider.isShortName( name );
		}

		@Override
		public String resolve(String shortName) {
			return AvailableDatastoreProvider.byShortName( shortName ).getDatastoreProviderClassName();
		}
	}
}
