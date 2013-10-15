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

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.hibernate.ogm.options.spi.MappingFactory;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.hibernate.ogm.options.spi.OptionsService;

/**
 * Provides read and write access to option contexts maintained at the session factory and session level.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Gunnar Morling
 */
public class OptionsServiceImpl implements OptionsService, ConfigurationBuilderService {

	private final MappingFactory<?> mappingFactory;
	private final OptionsContext globalContext;

	public OptionsServiceImpl(MappingFactory<?> factory, SessionFactoryImplementor sessionFactoryImplementor) {
		this.mappingFactory = factory;
		this.globalContext = new OptionsContext();
	}

	//OptionsService

	@Override
	public OptionsServiceContext context() {
		return new OptionsServiceContextImpl( globalContext );
	}

	@Override
	public OptionsServiceContext context(SessionImplementor session) {
		return new OptionsServiceContextWithSession( session );
	}

	//ConfigurationBuilderService

	@Override
	public GlobalContext<?, ?> getConfigurationBuilder() {
		return mappingFactory.createMapping( new ConfigurationContext( globalContext ) );
	}

	private static final class OptionsServiceContextImpl implements OptionsServiceContext {

		private final OptionsContext context;

		public OptionsServiceContextImpl(OptionsContext context) {
			this.context = context;
		}

		@Override
		public OptionsContainer getGlobalOptions() {
			return context.getGlobalOptions();
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
