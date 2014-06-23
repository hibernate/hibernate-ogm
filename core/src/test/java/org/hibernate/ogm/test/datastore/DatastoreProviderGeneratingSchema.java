/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.datastore;

import java.util.Iterator;

import org.hibernate.LockMode;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.StartStoppable;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.IdGeneratorKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.service.impl.LuceneBasedQueryParserService;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.util.ClosableIterator;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

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
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return LuceneBasedQueryParserService.class;
	}

	@Override
	public void start(Configuration configuration, SessionFactoryImplementor factory) {
		Iterator<Table> tables = configuration.getTableMappings();
		while ( tables.hasNext() ) {
			Table table = tables.next();
			if ( table.isPhysicalTable() ) {
				String tableName = table.getQuotedName();
				// do something with table
				Iterator<Column> columns = table.getColumnIterator();
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
		public void nextValue(IdGeneratorKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		}

		@Override
		public boolean supportsSequences() {
			return false;
		}

		@Override
		public GridType overrideType(Type type) {
			// No types to override
			return null;
		}

		@Override
		public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		}

		@Override
		public ClosableIterator<Tuple> executeBackendQuery(BackendCustomQuery customQuery, QueryParameters queryParameters) {
			return null;
		}

		@Override
		public ParameterMetadataBuilder getParameterMetadataBuilder() {
			return null;
		}
	}
}
