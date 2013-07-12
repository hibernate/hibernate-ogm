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
package org.hibernate.ogm.mapping.impl;

import java.util.Map;

import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.mapping.spi.MappingService;
import org.hibernate.ogm.service.impl.OptionalServiceInitiator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public final class MappingServiceInitiator extends OptionalServiceInitiator<MappingService> {

	public static final String MAPPING = "hibernate.ogm.mapping";

	public static final BasicServiceInitiator<MappingService> INSTANCE = new MappingServiceInitiator();

	private static final Log log = LoggerFactory.make();

	@Override
	protected MappingService buildServiceInstance(Map configurationValues, ServiceRegistryImplementor registry) {
		Object object = configurationValues.get( MAPPING );
		MappingFactory factory = findFactory( registry, object );
		return new MappingServiceImpl( factory, registry );
	}

	private MappingFactory findFactory(ServiceRegistryImplementor registry, Object object) {
		if ( object == null ) {
			return factory( registry );
		}
		else if ( object instanceof String ) {
			return factory( registry, (String) object );
		}
		else if ( object instanceof MappingContext ) {
			return factory( (MappingContext) object );
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	private MappingFactory factory(final MappingContext context) {
		return new MappingFactory() {
			@Override
			public Class getMappingType() {
				return null;
			}

			@Override
			public MappingContext createMappingContext() {
				return context;
			}
		};
	}

	private MappingFactory factory(ServiceRegistryImplementor registry) {
		DatastoreProvider datastoreProvider = registry.getService( DatastoreProvider.class );
		return factory( datastoreProvider.getDefaultMappingServiceFactory() );
	}

	private MappingFactory factory(ServiceRegistryImplementor registry, String factoryClassName) {
		Class<? extends MappingFactory> factoryClass = registry.getService( ClassLoaderService.class ).classForName( factoryClassName );
		return factory( factoryClass );
	}

	private MappingFactory factory(Class<? extends MappingFactory> factoryClass) {
		try {
			return factoryClass.newInstance();
		}
		catch ( InstantiationException e ) {
			throw log.cannotCreateMappingFactory( factoryClass, e );
		}
		catch ( IllegalAccessException e ) {
			throw log.cannotCreateMappingFactory( factoryClass, e );
		}
	}

	@Override
	protected BasicServiceInitiator<MappingService> backupInitiator() {
		return null;
	}

	@Override
	public Class<MappingService> getServiceInitiated() {
		return MappingService.class;
	}

}
