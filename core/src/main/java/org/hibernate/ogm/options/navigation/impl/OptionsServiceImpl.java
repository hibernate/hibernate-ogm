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
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Creates a service that can be called to retrieve the mapping context containing all the options.
 * <p>
 * Some options might be session dependent.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OptionsServiceImpl implements OptionsService {

	private final MappingFactory<?> mappingFactory;

	private final ServiceRegistryImplementor registry;

	private final SessionFactoryImplementor sessionFactoryImplementor;

	public OptionsServiceImpl(MappingFactory<?> factory, ServiceRegistryImplementor registry, SessionFactoryImplementor sessionFactoryImplementor) {
		this.mappingFactory = factory;
		this.registry = registry;
		this.sessionFactoryImplementor = sessionFactoryImplementor;
	}

	@Override
	public OptionsServiceContext context() {
		ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
		OptionsContext context = createContext( classLoaderService );
		return new OptionsServiceContextImpl( context );
	}

	@Override
	public OptionsServiceContext context(SessionImplementor session) {
		return new OptionsServiceContextWithSession( session );
	}

	private OptionsContext createContext(ClassLoaderService classLoaderService) {
		OptionsContext context = new OptionsContext();
		GlobalContext<?, ?> globalContext = mappingFactory.createMapping( context );
		initializeContext( classLoaderService, globalContext );
		return context;
	}

	private void initializeContext(ClassLoaderService classLoaderService, GlobalContext<?, ?> globalContext) {
		Map<String, EntityPersister> entityPersisters = sessionFactoryImplementor.getEntityPersisters();
		for ( EntityPersister persister : entityPersisters.values() ) {
			String entityName = persister.getEntityName();
			Class<Object> classForName = classLoaderService.classForName( entityName );
			globalContext.entity( classForName );
		}
	}

	private static final class OptionsServiceContextImpl implements OptionsServiceContext {
		private final OptionsContext context;

		public OptionsServiceContextImpl(OptionsContext context) {
			this.context = context;
		}

		@Override
		public OptionsContainer getGlobalOptions() {
			return this.context.getGlobalOptions();
		}

		@Override
		public OptionsContainer getEntityOptions(Class<?> entityType) {
			return context.getEntityOptions( entityType );
		}

		@Override
		public OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName) {
			return context.getPropertyOptions( entityType, propertyName );
		}
	}

	private static final class OptionsServiceContextWithSession implements OptionsServiceContext {

		public OptionsServiceContextWithSession(SessionImplementor session) {
		}

		@Override
		public OptionsContainer getGlobalOptions() {
			throw new UnsupportedOperationException( "OGM-343 Session specific options are not currently supported" );
		}

		@Override
		public OptionsContainer getEntityOptions(Class<?> entityType) {
			throw new UnsupportedOperationException( "OGM-343 Session specific options are not currently supported" );
		}

		@Override
		public OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName) {
			throw new UnsupportedOperationException( "OGM-343 Session specific options are not currently supported" );
		}
	}

}
