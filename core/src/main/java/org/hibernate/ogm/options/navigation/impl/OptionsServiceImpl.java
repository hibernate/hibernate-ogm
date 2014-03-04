/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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

import java.util.List;
import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSources;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Provides read access to option contexts maintained at the session factory and session level.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Gunnar Morling
 */
public class OptionsServiceImpl implements OptionsService, Configurable, ServiceRegistryAwareService {

	private static final Log log = LoggerFactory.make();

	private OptionsServiceContext sessionFactoryOptions;
	private ServiceRegistryImplementor registry;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.registry = serviceRegistry;
	}

	@Override
	public void configure(Map configurationValues) {
		ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationValues, classLoaderService );

		sessionFactoryOptions = new OptionsServiceContextImpl( OptionValueSources.getDefaultSources( propertyReader ) );
	}

	@Override
	public OptionsServiceContext context() {
		return sessionFactoryOptions;
	}

	@Override
	public OptionsServiceContext context(SessionImplementor session) {
		throw new UnsupportedOperationException( "OGM-343 Session specific options are not currently supported" );
	}

	private static class OptionsServiceContextImpl implements OptionsServiceContext {

		private final List<OptionValueSource> sources;

		public OptionsServiceContextImpl(List<OptionValueSource> sources) {
			this.sources = sources;
		}

		@Override
		public OptionsContext getGlobalOptions() {
			return OptionsContextImpl.forGlobal( sources );
		}

		@Override
		public OptionsContext getEntityOptions(Class<?> entityType) {
			return OptionsContextImpl.forEntity( sources, entityType );
		}

		@Override
		public OptionsContext getPropertyOptions(Class<?> entityType, String propertyName) {
			return OptionsContextImpl.forProperty( sources, entityType, propertyName );
		}
	}
}
