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
package org.hibernate.ogm.test.datastore;

import org.hibernate.LockMode;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.datastore.StartStoppable;
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
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

import java.util.Iterator;

/**
 * Example of datastore provider using metadata to generate some hypothetical
 * schema.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DatastoreProviderGeneratingSchema implements DatastoreProvider, StartStoppable {
	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return Dialect.class;
	}

	@Override
	public void start(Configuration configuration, SessionFactoryImplementor factory) {
		Iterator<Table> tables = configuration.getTableMappings();
		while ( tables.hasNext() ) {
			Table table = tables.next();
			if ( table.isPhysicalTable() ) {
				String tableName = table.getQuotedName();
				// do something with table
				Iterator<Column> columns = (Iterator<Column>) table.getColumnIterator();
				while ( columns.hasNext() ) {
					Column column = columns.next();
					String columnName = column.getCanonicalName();
					// do something with column
				}
				//TODO handle unique constraints?
			}
		}
		throw new RuntimeException("STARTED!");
	}

	@Override
	public void stop() {
		//not tested
		throw new RuntimeException("STOPPED!");
	}

	public static class Dialect implements GridDialect {

		public Dialect(DatastoreProviderGeneratingSchema provider) {

		}

		@Override
		public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public Tuple createTuple(EntityKey key) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void updateTuple(Tuple tuple, EntityKey key) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void removeTuple(EntityKey key) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public Association createAssociation(AssociationKey key) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void updateAssociation(Association association, AssociationKey key) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void removeAssociation(AssociationKey key) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
			return null;  //To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
			//To change body of implemented methods use File | Settings | File Templates.
		}

		@Override
		public GridType overrideType(Type type) {
			// No types to override
			return null;
		}

		@Override
		public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		}
	}
}
