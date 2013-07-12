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

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.mapping.context.GlobalContext;
import org.hibernate.ogm.mapping.impl.MappingContext.PropertyKey;
import org.hibernate.ogm.mapping.spi.MappingService;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MappingServiceImpl implements MappingService {

	private final MappingFactory mappingServiceFactory;

	private ServiceRegistryImplementor registry;

	private SessionFactoryImplementor sessionFactoryImplementor;

	public MappingServiceImpl(MappingFactory factory, ServiceRegistryImplementor registry) {
		this.mappingServiceFactory = factory;
		this.registry = registry;
	}

	@Override
	public void start(Configuration configuration, SessionFactoryImplementor sessionFactoryImplementor) {
		this.sessionFactoryImplementor = sessionFactoryImplementor;
	}

	@Override
	public void stop() {
	}

	@Override
	public MappingServiceContext context() {
		ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
		MappingContext context = createContext( classLoaderService );
		return new MappingServiceContextImpl( context );
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

	@Override
	public MappingServiceContext context(SessionImplementor session) {
		return new MappingServiceContextWithSession( session );
	}

	private final class MappingServiceContextImpl implements MappingServiceContext {
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

	private final class MappingServiceContextWithSession implements MappingServiceContext {

		private final SessionImplementor session;

		public MappingServiceContextWithSession(SessionImplementor session) {
			this.session = session;
		}

		@Override
		public OptionsContainer getGlobalOptions() {
			throw new UnsupportedOperationException();
		}

		@Override
		public OptionsContainer getEntityOptions(Class<?> entityType) {
			throw new UnsupportedOperationException();
		}

		@Override
		public OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName) {
			throw new UnsupportedOperationException();
		}
	}

}
