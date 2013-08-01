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
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

import java.util.Arrays;
import java.util.Map;

/**
 * Loads the appropriate {@link DatastoreProvider}. Driven by the {@link #DATASTORE_PROVIDER}
 * property which can receive
 * <ul>
 *   <li>a {@code DatastoreProvider} instance</li>
 *   <li>a {@code DatastoreProvider} class</li>
 *   <li>a string representing the {@code DatastoreProvider} class</li>
 *   <li>a string reprenseting one of the datastore provider shortcuts</li>
 * </ul>
 *
 * If the property is not set, Infinispan is used by default.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Davide D'Alto <davide@hibernate.org>
 */
public final class DatastoreProviderInitiator implements SessionFactoryServiceInitiator<DatastoreProvider> {

	public static final String DATASTORE_PROVIDER = "hibernate.ogm.datastore.provider";
	public static final DatastoreProviderInitiator INSTANCE = new DatastoreProviderInitiator();

	private static final Log log = LoggerFactory.make();
	private static final String DEFAULT_DATASTORE_PROVIDER_CLASS = "org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider";

	@Override
	public Class<DatastoreProvider> getServiceInitiated() {
		return DatastoreProvider.class;
	}

	@Override
	public DatastoreProvider initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry) {
		DatastoreProvider datastoreProvider = createDatastoreProvider( configuration.getProperties(), registry );
		log.useDatastoreProvider( datastoreProvider.getClass().getName() );
		return datastoreProvider;
	}

	@Override
	public DatastoreProvider initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry) {
		throw new UnsupportedOperationException( "Cannot create " + DatastoreProvider.class.getName() + " service using metadata" );
	}

	private DatastoreProvider createDatastoreProvider(Map<?, ?> configuration, ServiceRegistryImplementor registry) {
		Object datastoreProviderProperty = configuration.get( DATASTORE_PROVIDER );
		if ( datastoreProviderProperty == null ) {
			return defaultDatastoreProvider( registry );
		}
		else if ( datastoreProviderProperty instanceof DatastoreProvider ) {
			return (DatastoreProvider) datastoreProviderProperty;
		}
		else if ( datastoreProviderProperty instanceof String ) {
			return createDatastore( registry, (String) datastoreProviderProperty );
		}
		else if ( datastoreProviderProperty instanceof Class<?> ) {
			return create( registry, (Class<?>) datastoreProviderProperty );
		}
		throw log.unknownDatastoreManagerType( datastoreProviderProperty.getClass().getName() );
	}

	private DatastoreProvider createDatastore(ServiceRegistryImplementor registry, String managerProperty) {
		Class<?> dataStoreProviderClass = findDataStoreProviderClass( registry, managerProperty );
		return create( registry, dataStoreProviderClass );
	}

	private DatastoreProvider create(ServiceRegistryImplementor registry, Class<?> datastoreProviderClass) {
		try {
			validate( datastoreProviderClass );
			return (DatastoreProvider) datastoreProviderClass.newInstance();
		}
		catch (InstantiationException e) {
			throw log.unableToInstantiateDatastoreManager( datastoreProviderClass.getName(), e );
		}
		catch (IllegalAccessException e) {
			throw log.unableToInstantiateDatastoreManager( datastoreProviderClass.getName(), e );
		}
	}

	private DatastoreProvider defaultDatastoreProvider(ServiceRegistryImplementor registry) {
		try {
			ClassLoaderService service = registry.getService( ClassLoaderService.class );
			Class<?> datastoreProviderClass = service.classForName( DEFAULT_DATASTORE_PROVIDER_CLASS );
			return (DatastoreProvider) datastoreProviderClass.newInstance();
		}
		catch (ClassLoadingException e) {
			throw log.noDatastoreConfigured();
		}
		catch (InstantiationException e) {
			throw log.unableToInstantiateDatastoreManager( DEFAULT_DATASTORE_PROVIDER_CLASS, e );
		}
		catch (IllegalAccessException e) {
			throw log.unableToInstantiateDatastoreManager( DEFAULT_DATASTORE_PROVIDER_CLASS, e );
		}
	}

	private void validate(Class<?> datastoreProviderClass) {
		if ( !( DatastoreProvider.class.isAssignableFrom( datastoreProviderClass ) ) ) {
			throw log.notADatastoreManager( datastoreProviderClass.getName() );
		}
	}

	private Class<?> findDataStoreProviderClass(ServiceRegistryImplementor registry, final String managerPropertyValue) {
		try {
			String datastoreProviderClassName = dataStoreProviderClassName( managerPropertyValue );
			return registry.getService( ClassLoaderService.class ).classForName( datastoreProviderClassName );
		}
		catch (Exception e) {
			throw log.datastoreClassCannotBeFound( managerPropertyValue, Arrays.toString( AvailableDatastoreProvider.values() ) );
		}
	}

	private String dataStoreProviderClassName(final String managerPropertyValue) {
		if ( isValidShortcut( managerPropertyValue ) ) {
			return AvailableDatastoreProvider.valueOf( managerPropertyValue.toUpperCase() ).getDatastoreProviderClassName();
		}
		else {
			return managerPropertyValue;
		}
	}

	private boolean isValidShortcut(String shortcut) {
		for ( AvailableDatastoreProvider provider : AvailableDatastoreProvider.values() ) {
			if ( provider.name().equalsIgnoreCase( shortcut ) ) {
				return true;
			}
		}
		return false;
	}

}
