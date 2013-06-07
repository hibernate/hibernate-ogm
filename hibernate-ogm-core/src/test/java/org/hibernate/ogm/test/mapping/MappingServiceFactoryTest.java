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
package org.hibernate.ogm.test.mapping;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.datastore.impl.DatastoreProviderInitiator;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.GridDialectLogger;
import org.hibernate.ogm.mapping.impl.ConfigurationProxyFactory;
import org.hibernate.ogm.mapping.impl.MappingContext;
import org.hibernate.ogm.mapping.impl.MappingFactory;
import org.hibernate.ogm.mapping.impl.MappingServiceInitiator;
import org.hibernate.ogm.mapping.spi.MappingService;
import org.hibernate.ogm.mapping.spi.MappingService.MappingServiceContext;
import org.hibernate.ogm.test.mapping.SampleMappingModel.SampleMapping;
import org.hibernate.ogm.test.mapping.option.NameExampleOption;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class MappingServiceFactoryTest extends OgmTestCase {

	private final LeakingDataStoreProvider provider = new LeakingDataStoreProvider();

	@Test
	public void testMappingContextAsProperty() throws Exception {
		assertThat( provider.context.getEntityOptions( SampleEntity.class ).asList() )
			.containsExactly( new NameExampleOption( "PROGRAMMATIC" ) );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {};
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );

		SampleMappingFactory factory = new SampleMappingFactory();
		MappingContext context = factory.createMappingContext();
		SampleMapping mapping = ConfigurationProxyFactory.get( factory.getMappingType(), context );
		mapping.entity( SampleEntity.class ).name( "PROGRAMMATIC" );

		cfg.getProperties().put( MappingServiceInitiator.MAPPING, context );
		cfg.getProperties().put( DatastoreProviderInitiator.DATASTORE_PROVIDER, provider );
	}

	private static class SampleEntity {
	}

	private static class LeakingDataStoreProvider implements DatastoreProvider, ServiceRegistryAwareService {

		private MappingServiceContext context;

		@Override
		public Class<? extends GridDialect> getDefaultDialect() {
			// Any dialect will do for the purpose of this test
			return GridDialectLogger.class;
		}

		@Override
		public void injectServices(ServiceRegistryImplementor serviceRegistry) {
			MappingService service = serviceRegistry.getService( MappingService.class );
			context = service.context();
		}

		@Override
		public Class<? extends MappingFactory<?>> getDefaultMappingServiceFactory() {
			return null;
		}

	}

}
