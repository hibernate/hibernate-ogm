/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.batch;

import java.util.Map;

import org.fest.assertions.Assertions;
import org.hibernate.Session;
import org.hibernate.ogm.backendtck.simpleentity.Hypothesis;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * Test that the methods to execute batch operations are called when a dialect is a {@link BatchableGridDialect}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
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
		session.beginTransaction();
		session.persist( new Hypothesis( "hypo-1" ) );
		session.flush();
		session.getTransaction().commit();
		session.close();

		Assertions.assertThat( batchExecuted ).as( "Batched operations should be executed during flush" ).isTrue();
	}

	@Test
	public void testImplicitFlushEvent() throws Exception {
		final Session session = openSession();
		session.beginTransaction();
		session.persist( new Hypothesis( "hypo-1" ) );
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
	protected void configure(Map<String, Object> settings) {
		settings.put( OgmProperties.DATASTORE_PROVIDER, SampleBatchableDatastoreProvider.class.getName() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Hypothesis.class };
	}

	public static class SampleBatchableDatastoreProvider extends BaseDatastoreProvider {

		@Override
		public Class<? extends GridDialect> getDefaultDialect() {
			return SampleBatchableDialect.class;
		}
	}

	public static class SampleBatchableDialect extends BaseGridDialect implements BatchableGridDialect {

		public SampleBatchableDialect(SampleBatchableDatastoreProvider provider) {
		}

		@Override
		public void executeBatch(OperationsQueue queue) {
			batchExecuted = true;
		}

		@Override
		public Tuple getTuple(EntityKey key, OperationContext tupleContext) {
			return null;
		}

		@Override
		public Tuple createTuple(EntityKey key, OperationContext tupleContext) {
			return new Tuple();
		}

		@Override
		public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) {
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
		public Number nextValue(NextValueRequest request) {
			return null;
		}

		@Override
		public void forEachTuple(ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata) {
		}

		@Override
		public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
			return false;
		}
	}
}
