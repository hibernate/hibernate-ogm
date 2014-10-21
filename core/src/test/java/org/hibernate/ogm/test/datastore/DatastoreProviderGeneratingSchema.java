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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.BaseSchemaDefiner;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.persister.entity.Lockable;

/**
 * Example of datastore provider using metadata to generate some hypothetical
 * schema.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class DatastoreProviderGeneratingSchema extends BaseDatastoreProvider {

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return Dialect.class;
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return TestSchemaDefiner.class;
	}

	public static class TestSchemaDefiner extends BaseSchemaDefiner {

		@Override
		public void initializeSchema(Configuration configuration, SessionFactoryImplementor factory) {
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
	}

	public static class Dialect extends BaseGridDialect {

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
		public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
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
		public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		}

		@Override
		public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		}

		@Override
		public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
			return false;
		}

		@Override
		public Number nextValue(NextValueRequest request) {
			return null;
		}

		@Override
		public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		}
	}
}
