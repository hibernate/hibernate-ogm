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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.LockMode;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.lock.LockingStrategy;
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
import org.hibernate.ogm.mapping.impl.MappingFactory;
import org.hibernate.ogm.mapping.impl.MappingServiceInitiator;
import org.hibernate.ogm.mapping.spi.MappingService;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.test.mapping.annotation.NameExample;
import org.hibernate.ogm.test.mapping.option.NameExampleOption;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class MappingServiceTest extends OgmTestCase {

	private final LeakingDataStoreProvider provider = new LeakingDataStoreProvider();

	@Test
	public void testMappingContextAsProperty() throws Exception {
		assertThat( provider.service.context().getEntityOptions( SampleEntity.class ).asList() ).containsExactly(
				new NameExampleOption( "Experiment" ) );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { SampleEntity.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put( MappingServiceInitiator.MAPPING, SampleMappingFactory.class.getName() );
		cfg.getProperties().put( DatastoreProviderInitiator.DATASTORE_PROVIDER, provider );
	}

	@Entity
	@NameExample("Experiment")
	public static class SampleEntity {
		@Id
		@GeneratedValue
		public Long id;
	}

	public static class LeakingDataStoreProvider implements DatastoreProvider, ServiceRegistryAwareService {

		private MappingService service;

		@Override
		public Class<? extends GridDialect> getDefaultDialect() {
			return NopeDialect.class;
		}

		@Override
		public void injectServices(ServiceRegistryImplementor serviceRegistry) {
			service = serviceRegistry.getService( MappingService.class );
		}

		@Override
		public Class<? extends MappingFactory<?>> getDefaultMappingServiceFactory() {
			return SampleMappingFactory.class;
		}

	}

	public static class NopeDialect implements GridDialect {

		public NopeDialect(LeakingDataStoreProvider provider) {
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
		public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
			return null;
		}

		@Override
		public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		}

		@Override
		public void removeTuple(EntityKey key, TupleContext tupleContext) {
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
}
