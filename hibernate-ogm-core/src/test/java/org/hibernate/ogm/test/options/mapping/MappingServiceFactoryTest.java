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
package org.hibernate.ogm.test.options.mapping;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.LockMode;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.impl.DatastoreProviderInitiator;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.options.navigation.impl.GenericNoSqlMappingFactory;
import org.hibernate.ogm.options.navigation.impl.MappingContext;
import org.hibernate.ogm.options.navigation.impl.MappingServiceInitiator;
import org.hibernate.ogm.options.spi.MappingFactory;
import org.hibernate.ogm.options.spi.MappingService;
import org.hibernate.ogm.options.spi.MappingService.MappingServiceContext;
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.hibernate.ogm.service.impl.LuceneBasedQueryParserService;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.ogm.test.options.examples.NameExampleOption;
import org.hibernate.ogm.test.options.mapping.SampleMappingModel.SampleEntityContextImpl;
import org.hibernate.ogm.test.options.mapping.SampleMappingModel.SampleGlobalContext;
import org.hibernate.ogm.test.options.mapping.SampleMappingModel.SampleGlobalContextImpl;
import org.hibernate.ogm.test.options.mapping.SampleMappingModel.SamplePropertyContextImpl;
import org.hibernate.ogm.test.utils.OgmTestCase;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;
import org.junit.Test;

/**
 * Used for testing, allow access to the mapping context once it has been retrieved.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class MappingServiceFactoryTest extends OgmTestCase {

	@Test
	public void testMappingContextAsProperty() throws Exception {
		LeakingDataStoreProvider leakingProvider = (LeakingDataStoreProvider) registry().getService( DatastoreProvider.class );
		MappingServiceContext context = leakingProvider.context;
		assertThat( context ).as( "MappingContext not injected" ).isNotNull();
		assertThat( asSet( context.getEntityOptions( SampleEntity.class ) ) ).containsOnly( new NameExampleOption( "PROGRAMMATIC" ) );
	}

	private ServiceRegistryImplementor registry() {
		return ( (SessionFactoryImplementor) sessions ).getServiceRegistry();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SampleEntity.class };
	}

	/**
	 * Create a maping context initializing it with some property that we are going to check in the tests
	 */
	public static class LeakingMappingFactory implements MappingFactory<SampleGlobalContext> {

		@Override
		public SampleGlobalContext createMapping(MappingContext context) {
			SampleGlobalContext mapping = context.createGlobalContext( SampleGlobalContextImpl.class, SampleEntityContextImpl.class, SamplePropertyContextImpl.class );
			mapping.entity( SampleEntity.class ).name( "PROGRAMMATIC" );
			return mapping;
		}

	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put( MappingServiceInitiator.MAPPING, LeakingMappingFactory.class.getName() );
		cfg.getProperties().put( DatastoreProviderInitiator.DATASTORE_PROVIDER, LeakingDataStoreProvider.class.getName() );
	}

	private Set<Option<?, ?>> asSet(OptionsContainer container) {
		Set<Option<?, ?>> options = new HashSet<Option<?, ?>>();
		for ( Option<?, ?> option : container ) {
			options.add( option );
		}
		return options;
	}

	@Entity
	public static class SampleEntity {

		@Id
		public long id;
	}

	/**
	 * Dialect for the {@link LeakingDataStoreProvider}.
	 */
	public static class LeakingDatasStoreDialect implements GridDialect {

		public LeakingDatasStoreDialect(LeakingDataStoreProvider provider) {
		}

		@Override
		public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
			return null;
		}

		@Override
		public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
			return null;
		}

		@Override
		public Tuple createTuple(EntityKey key) {
			return null;
		}

		@Override
		public void updateTuple(Tuple tuple, EntityKey key) {
		}

		@Override
		public void removeTuple(EntityKey key) {
		}

		@Override
		public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
			return null;
		}

		@Override
		public Association createAssociation(AssociationKey key) {
			return null;
		}

		@Override
		public void updateAssociation(Association association, AssociationKey key) {
		}

		@Override
		public void removeAssociation(AssociationKey key) {
		}

		@Override
		public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
			return null;
		}

		@Override
		public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		}

		@Override
		public GridType overrideType(Type type) {
			return null;
		}

		@Override
		public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		}
	}

	/**
	 * A provider used for testing that exposes the mapping context obtained from the service registry.
	 */
	public static class LeakingDataStoreProvider implements DatastoreProvider, ServiceRegistryAwareService {

		private MappingServiceContext context;

		@Override
		public Class<? extends GridDialect> getDefaultDialect() {
			// Any dialect will do for the purpose of this test
			return LeakingDatasStoreDialect.class;
		}

		@Override
		public void injectServices(ServiceRegistryImplementor serviceRegistry) {
			MappingService mappingService = serviceRegistry.getService( MappingService.class );
			context = mappingService.context();
		}

		@Override
		public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
			return LuceneBasedQueryParserService.class;
		}

		@Override
		public Class<? extends MappingFactory<?>> getMappingFactoryType() {
			return GenericNoSqlMappingFactory.class;
		}

	}

}
