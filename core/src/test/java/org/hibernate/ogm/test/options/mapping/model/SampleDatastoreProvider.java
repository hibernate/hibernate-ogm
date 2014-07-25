/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.mapping.model;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.id.spi.NextValueRequest;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.persister.entity.Lockable;

/**
 * @author Gunnar Morling
 */
public class SampleDatastoreProvider extends BaseDatastoreProvider {

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return SampleDialect.class;
	}

	public static class SampleDialect extends BaseGridDialect {

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
		public Tuple createTupleAssociation(AssociationKey associationKey, AssociationContext associationContext, RowKey rowKey) {
			return null;
		}

		@Override
		public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
			return false;
		}

		@Override
		public Number nextValue(NextValueRequest request) {
			return null;
		}

		@Override
		public boolean supportsSequences() {
			return false;
		}

		@Override
		public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		}
	}
}
