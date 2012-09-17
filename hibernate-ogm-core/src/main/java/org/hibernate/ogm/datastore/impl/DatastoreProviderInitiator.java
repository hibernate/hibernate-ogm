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
import org.hibernate.ogm.service.impl.OptionalServiceInitiator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.Arrays;
import java.util.Map;

/**
 * Load DatastoreManager from DATASTORE_MANAGER as string, class or DatastoreManager instance.
 * If the property is not set, Infinispan is used by default.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DatastoreProviderInitiator extends OptionalServiceInitiator<DatastoreProvider> {
	public static final String DATASTORE_PROVIDER = "hibernate.ogm.datastore.provider";
	private static final Log log = LoggerFactory.make();

	public static final DatastoreProviderInitiator INSTANCE = new DatastoreProviderInitiator();

	@Override
	public DatastoreProvider buildServiceInstance(Map configurationValues, ServiceRegistryImplementor registry) {
		Object managerProperty = configurationValues.get( DATASTORE_PROVIDER );
		Class<?> managerClass = null;
		if ( managerProperty instanceof String ) {
			final String managerPropertyValue = (String) managerProperty;
			String datastoreProviderClass;
			try {
				if ( isValidShortcut( managerPropertyValue ) ) {
					datastoreProviderClass = AvailableDatastoreProvider.valueOf( managerPropertyValue.toUpperCase() )
							.getDatastoreProviderClassName();
				}
				else {
					datastoreProviderClass = managerPropertyValue;
				}
				managerClass = registry.getService( ClassLoaderService.class ).classForName( datastoreProviderClass );
			}
			catch ( Exception e ) {
				throw log.datastoreClassCannotBeFound( managerPropertyValue, Arrays.toString( AvailableDatastoreProvider.values() ) );
			}
		}
		else if ( managerProperty instanceof Class ) {
			managerClass = (Class<?>) managerProperty;
		}
		if ( managerClass != null ) {
			if ( !( DatastoreProvider.class.isAssignableFrom( managerClass ) ) ) {
				throw log.notADatastoreManager( managerClass.getName() );
			}
			try {
				return logAndReturn( (DatastoreProvider) managerClass.newInstance() );
			}
			catch ( Exception e ) {
				throw log.unableToInstantiateDatastoreManager( managerClass.getName(), e );
			}
		}

		if ( managerProperty instanceof DatastoreProvider ) {
			return logAndReturn( (DatastoreProvider) managerProperty );
		}
		else if ( managerProperty == null ) {
			return logAndReturn( guessDatastoreProvider( registry.getService( ClassLoaderService.class ) ) );
		}
		else {
			throw log.unknownDatastoreManagerType( managerProperty.getClass().getName() );
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

	@Override
	protected BasicServiceInitiator<DatastoreProvider> backupInitiator() {
		return null;
	}

	private DatastoreProvider logAndReturn(DatastoreProvider datastoreProvider) {
		log.useDatastoreProvider( datastoreProvider.getClass().getName() );
		return datastoreProvider;
	}

	private DatastoreProvider guessDatastoreProvider(ClassLoaderService service) {
		Class<?> managerClass = null;
		try {
			managerClass = service.classForName(
					"org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider"
			);
		}
		catch ( Exception e ) {

		}
		if ( managerClass != null ) {
			try {
				return (DatastoreProvider) managerClass.newInstance();
			}
			catch ( Exception e ) {
				throw log.unableToInstantiateDatastoreManager( managerClass.getName(), e );
			}
		}
		throw log.noDatastoreConfigured();
	}

	@Override
	public Class<DatastoreProvider> getServiceInitiated() {
		return DatastoreProvider.class;
	}
}
