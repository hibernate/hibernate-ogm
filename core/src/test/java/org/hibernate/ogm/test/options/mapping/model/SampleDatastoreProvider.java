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
package org.hibernate.ogm.test.options.mapping.model;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.TupleIterator;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.query.NoOpParameterMetadataBuilder;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

/**
 * @author Gunnar Morling
 */
public class SampleDatastoreProvider implements DatastoreProvider {

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return SampleDialect.class;
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		throw new UnsupportedOperationException();
	}

	public static class SampleDialect implements GridDialect {

		public SampleDialect(SampleDatastoreProvider provider) {
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
		public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
			return null;
		}

		@Override
		public void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext) {
		}

		@Override
		public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		}

		@Override
		public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
			return null;
		}

		@Override
		public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
			return false;
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

		@Override
		public TupleIterator executeBackendQuery(BackendCustomQuery customQuery, QueryParameters queryParameters, EntityKeyMetadata[] metadatas) {
			return null;
		}

		@Override
		public ParameterMetadataBuilder getParameterMetadataBuilder() {
			return new NoOpParameterMetadataBuilder();
		}
	}
}
