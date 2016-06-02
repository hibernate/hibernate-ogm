/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.batch;

import static org.hibernate.ogm.util.impl.ArrayHelper.EMPTY_STRING_ARRAY;
import static org.hibernate.ogm.utils.GridDialectOperationContexts.emptyTupleContext;
import static org.hibernate.ogm.utils.GridDialectOperationContexts.emptyAssociationContext;

import org.fest.assertions.Assertions;
import org.hibernate.HibernateException;
import org.hibernate.ogm.dialect.batch.spi.GroupedChangesToEntityOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateTupleOperation;
import org.hibernate.ogm.dialect.batch.spi.Operation;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.batch.spi.RemoveTupleOperation;
import org.hibernate.ogm.model.impl.DefaultAssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultAssociationKeyMetadata;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.AssociationType;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the methods in the {@link OperationsQueue}
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class OperationsQueueTest {

	private OperationsQueue queue;

	@Before
	public void init() {
		queue = new OperationsQueue();
	}

	@Test(expected = HibernateException.class)
	public void testAddCauseExceptionWhenQueueIsClosed() throws Exception {
		queue.close();
		queue.add( new RemoveTupleOperation( null, emptyTupleContext() ) );
	}

	@Test(expected = HibernateException.class)
	public void testAddUpdateTupleCauseExceptionWhenQueueIsClosed() throws Exception {
		queue.close();
		queue.add( new InsertOrUpdateTupleOperation( null, null, emptyTupleContext() ) );
	}

	@Test(expected = HibernateException.class)
	public void testPollCauseExceptionWhenQueueIsClosed() throws Exception {
		queue.close();
		queue.poll();
	}

	@Test
	public void testContainsKeyWhenAddingUpdateTupleOperation() throws Exception {
		EntityKey key = entityKey();
		InsertOrUpdateTupleOperation expected = new InsertOrUpdateTupleOperation( null, key, emptyTupleContext() );
		queue.add( expected );

		Assertions.assertThat( queue.isInTheInsertionQueue( key ) ).isTrue();
	}

	@Test
	public void testContainsKeyIsFalseWhenAddingRemoveTupleOperation() throws Exception {
		EntityKey key = entityKey();
		RemoveTupleOperation expected = new RemoveTupleOperation( key, emptyTupleContext() );
		queue.add( expected );

		Assertions.assertThat( queue.isInTheInsertionQueue( key ) ).isFalse();
	}

	@Test
	public void testAddRemoveTupleOperation() throws Exception {
		EntityKey key = entityKey();
		RemoveTupleOperation expected = new RemoveTupleOperation( key, emptyTupleContext() );
		queue.add( expected );

		Assertions.assertThat( queue.poll() ).isEqualTo( expected );
	}

	@Test
	public void testAddUpdateTupleOperation() throws Exception {
		EntityKey key = entityKey();
		InsertOrUpdateTupleOperation insertOrUpdate = new InsertOrUpdateTupleOperation( null, key, emptyTupleContext() );
		queue.add( insertOrUpdate );

		Operation operation = queue.poll();
		Assertions.assertThat( operation ).isInstanceOf( GroupedChangesToEntityOperation.class );

		GroupedChangesToEntityOperation groupedOperation = (GroupedChangesToEntityOperation) operation;
		Assertions.assertThat( groupedOperation.getOperations().poll() ).isEqualTo( insertOrUpdate );
	}

	@Test
	public void testAddUpdateTupleAndUpdateAssociationOperation() throws Exception {
		EntityKey key = entityKey();
		InsertOrUpdateTupleOperation insertOrUpdateTuple = new InsertOrUpdateTupleOperation( null, key, emptyTupleContext() );
		queue.add( insertOrUpdateTuple );

		InsertOrUpdateAssociationOperation insertOrUpdateAssociation = new InsertOrUpdateAssociationOperation( null, getAssociationKey( key ),
				emptyAssociationContext() );
		queue.add( insertOrUpdateAssociation );

		Assertions.assertThat( queue.size() ).isEqualTo( 1 );

		Operation operation = queue.poll();
		Assertions.assertThat( operation ).isInstanceOf( GroupedChangesToEntityOperation.class );

		GroupedChangesToEntityOperation groupedOperation = (GroupedChangesToEntityOperation) operation;
		Assertions.assertThat( groupedOperation.getOperations().poll() ).isEqualTo( insertOrUpdateTuple );
		Assertions.assertThat( groupedOperation.getOperations().poll() ).isEqualTo( insertOrUpdateAssociation );
	}

	@Test
	public void testEmptyQueueSize() throws Exception {
		Assertions.assertThat( queue.size() ).isEqualTo( 0 );
	}

	@Test
	public void testQueueSizeWhenAddingUpdateTupleOperation() throws Exception {
		queue.add( new InsertOrUpdateTupleOperation( null, entityKey(), emptyTupleContext() ) );

		Assertions.assertThat( queue.size() ).isEqualTo( 1 );
	}

	@Test
	public void testQueueSizeWhenAddingRemoveTupleOperation() throws Exception {
		queue.add( new RemoveTupleOperation( entityKey(), emptyTupleContext() ) );

		Assertions.assertThat( queue.size() ).isEqualTo( 1 );
	}

	private EntityKey entityKey() {
		EntityKeyMetadata keyMetadata = new DefaultEntityKeyMetadata( "MetadataTable", new String[] {} );
		EntityKey key = new EntityKey( keyMetadata, new Object[] {} );
		return key;
	}

	private AssociationKey getAssociationKey(EntityKey entityKey) {
		String[] columnNames = new String[]{ "column1", "column2" };
		AssociationKeyMetadata keyMetadata = new DefaultAssociationKeyMetadata.Builder()
				.table( "MetadataTableAssociation" )
				.columnNames( columnNames )
				.rowKeyColumnNames( columnNames )
				.associatedEntityKeyMetadata( new DefaultAssociatedEntityKeyMetadata( EMPTY_STRING_ARRAY, null ) )
				.inverse( false )
				.collectionRole( "collectionRole" )
				.associationKind( AssociationKind.ASSOCIATION )
				.associationType( AssociationType.BAG )
				.build();
		return new AssociationKey( keyMetadata, columnNames, entityKey );
	}
}
