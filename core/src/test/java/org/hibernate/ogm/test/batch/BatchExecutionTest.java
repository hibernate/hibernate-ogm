/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.batch;

import org.fest.assertions.Assertions;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.backendtck.simpleentity.Hypothesis;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.BatchableGridDialect;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.batch.OperationsQueue;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.IdGeneratorKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.util.ClosableIterator;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;
import org.junit.Before;
import org.junit.Test;

/**
 * Test that the methods to execute batch operations are called when a dialect is a {@link BatchableGridDialect}
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class BatchExecutionTest extends OgmTestCase {

	static boolean batchExecuted = false;


	@Before
	public void before() {
		batchExecuted = false;
	}

	@Test
	public void testExplicitFlushEvent() throws Exception {
		final Session session = openSession();
		session.flush();
		session.close();

		Assertions.assertThat( batchExecuted ).as( "Batched operations should be executed during flush" ).isTrue();
	}

	@Test
	public void testImplicitFlushEvent() throws Exception {
		final Session session = openSession();
		session.beginTransaction();
		session.getTransaction().commit();
		session.close();

		Assertions.assertThat( batchExecuted ).as( "Batched operations should be executed during commit" ).isTrue();
	}

	@Test
	public void testBatchNotExecuted() throws Exception {
		final Session session = openSession();
		session.close();

		Assertions.assertThat( batchExecuted ).as( "Unexpected execution of batched operations" ).isFalse();
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( OgmProperties.DATASTORE_PROVIDER, SampleBatchableDatastoreProvider.class.getName() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Hypothesis.class };
	}

	public static class SampleBatchableDatastoreProvider implements DatastoreProvider {

		@Override
		public Class<? extends GridDialect> getDefaultDialect() {
			return SampleBatchableDialect.class;
		}

		@Override
		public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
			return null;
		}

	}

	public static class SampleBatchableDialect implements BatchableGridDialect {

		public SampleBatchableDialect(SampleBatchableDatastoreProvider provider) {
		}

		@Override
		public void executeBatch(OperationsQueue queue) {
			batchExecuted = true;
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
		public void nextValue(IdGeneratorKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		}

		@Override
		public boolean supportsSequences() {
			return false;
		}

		@Override
		public GridType overrideType(Type type) {
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
		public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
			return false;
		}

		@Override
		public ParameterMetadataBuilder getParameterMetadataBuilder() {
			return null;
		}
	}
}
