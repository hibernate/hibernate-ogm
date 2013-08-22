/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.options.navigation.impl;

import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.hibernate.ogm.options.spi.MappingFactory;
import org.hibernate.ogm.options.spi.MappingService;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Creates a service that can be called to retrieve the mapping context containg all the options.
 * <p>
 * Some options might be session dependent.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MappingServiceImpl implements MappingService {

	private final MappingFactory<?> mappingServiceFactory;

	private ServiceRegistryImplementor registry;

	private SessionFactoryImplementor sessionFactoryImplementor;

	public MappingServiceImpl(MappingFactory<?> factory, ServiceRegistryImplementor registry, SessionFactoryImplementor sessionFactoryImplementor) {
		this.mappingServiceFactory = factory;
		this.registry = registry;
		this.sessionFactoryImplementor = sessionFactoryImplementor;
	}

	@Override
	public MappingServiceContext context() {
		ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
		MappingContext context = createContext( classLoaderService );
		return new MappingServiceContextImpl( context );
	}

	@Override
	public MappingServiceContext context(SessionImplementor session) {
		return new MappingServiceContextWithSession( session );
	}

	private MappingContext createContext(ClassLoaderService classLoaderService) {
		MappingContext context = mappingServiceFactory.createMappingContext();
		if ( mappingServiceFactory.getMappingType() != null ) {
			GlobalContext<?, ?, ?> globalContext = (GlobalContext<?, ?, ?>) ConfigurationProxyFactory.get( mappingServiceFactory.getMappingType(), context );
			Map<String, EntityPersister> entityPersisters = sessionFactoryImplementor.getEntityPersisters();
			for ( EntityPersister persister : entityPersisters.values() ) {
				String entityName = persister.getEntityName();
				Class<Object> classForName = classLoaderService.classForName( entityName );
				globalContext.entity( classForName );
			}
		}
		return context;
	}

	private static final class MappingServiceContextImpl implements MappingServiceContext {
		private final MappingContext context;

		public MappingServiceContextImpl(MappingContext context) {
			this.context = context;
		}

		@Override
		public OptionsContainer getGlobalOptions() {
			return this.context.getGlobalOptions();
		}

		@Override
		public OptionsContainer getEntityOptions(Class<?> entityType) {
			Map<Class<?>, OptionsContainer> optionsPerEntity = context.getOptionsPerEntity();
			if ( optionsPerEntity.containsKey( entityType ) ) {
				return optionsPerEntity.get( entityType );
			}
			else {
				return EmptyOptionsContainer.INSTANCE;
			}
		}

		@Override
		public OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName) {
			Map<PropertyKey, OptionsContainer> optionsPerProperty = this.context.getOptionsPerProperty();
			PropertyKey key = new PropertyKey( entityType, propertyName );
			if ( optionsPerProperty.containsKey( key ) ) {
				return optionsPerProperty.get( key );
			}
			else {
				return EmptyOptionsContainer.INSTANCE;
			}
		}

	}

	private static final class MappingServiceContextWithSession implements MappingServiceContext {

		public MappingServiceContextWithSession(SessionImplementor session) {
		}

		@Override
		public OptionsContainer getGlobalOptions() {
			throw new UnsupportedOperationException( "Session specific options are not currently supported" );
		}

		@Override
		public OptionsContainer getEntityOptions(Class<?> entityType) {
			throw new UnsupportedOperationException( "Session specific options are not currently supported" );
		}

		@Override
		public OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName) {
			throw new UnsupportedOperationException( "Session specific options are not currently supported" );
		}
	}

}
