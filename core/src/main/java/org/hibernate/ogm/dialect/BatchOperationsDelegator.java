/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.batch.OperationsQueue;
import org.hibernate.ogm.dialect.batch.RemoveAssociationOperation;
import org.hibernate.ogm.dialect.batch.RemoveTupleOperation;
import org.hibernate.ogm.dialect.batch.UpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.UpdateTupleOperation;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.id.spi.NextValueRequest;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

/**
 * Wraps a {@link BatchableGridDialect} intercepting the operation and populating the queue that the delegate
 * will use to execute operations in batch.
 * <p>
 * The {@link TupleContext} and {@link AssociationContext} are also populated with the {@link OperationsQueue}
 * before looking for element in the db. This way the underlying datastore can make assumptions about elements
 * that are in the queue but not in the db.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class BatchOperationsDelegator implements GridDialect {

	// The threadlocal is properly set and cleaned in a try / catch by {@link org.hibernate.ogm.service.impl.BatchManagerEventListener}
	// if used elsewhere, apply the same pattern
	private final ThreadLocal<OperationsQueue> operationQueueLocal = new ThreadLocal<OperationsQueue>();

	private final BatchableGridDialect dialect;

	public BatchOperationsDelegator(BatchableGridDialect dialect) {
		this.dialect = dialect;
	}

	public void prepareBatch() {
		operationQueueLocal.set( new OperationsQueue() );
	}

	private boolean isBatchDisabled() {
		return getOperationQueue().isClosed();
	}

	public void clearBatch() {
		operationQueueLocal.remove();
	}

	private OperationsQueue getOperationQueue() {
		OperationsQueue operationsQueue = operationQueueLocal.get();
		if ( operationsQueue == null ) {
			return OperationsQueue.CLOSED_QUEUE;
		}
		else {
			return operationsQueue;
		}
	}

	public void executeBatch() {
		dialect.executeBatch( getOperationQueue() );
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		return dialect.getLockingStrategy( lockable, lockMode );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		TupleContext contextWithQueue = new TupleContext(
				tupleContext,
				getOperationQueue()
		);

		return dialect.getTuple( key, contextWithQueue );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		return dialect.createTuple( key, tupleContext );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		if ( isBatchDisabled() ) {
			dialect.updateTuple( tuple, key, tupleContext );
		}
		else {
			getOperationQueue().add( new UpdateTupleOperation( tuple, key, tupleContext ) );
		}
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		if ( isBatchDisabled() ) {
			dialect.removeTuple( key, tupleContext );
		}
		else {
			getOperationQueue().add( new RemoveTupleOperation( key, tupleContext ) );
		}
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		return dialect.getAssociation( key, withQueue( associationContext ) );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		return dialect.createAssociation( key, withQueue( associationContext ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext) {
		if ( isBatchDisabled() ) {
			dialect.updateAssociation( association, key, withQueue( associationContext ) );
		}
		else {
			getOperationQueue().add( new UpdateAssociationOperation( association, key, withQueue( associationContext ) ) );
		}
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		if ( isBatchDisabled() ) {
			dialect.removeAssociation( key, withQueue( associationContext ) );
		}
		else {
			getOperationQueue().add( new RemoveAssociationOperation( key, withQueue( associationContext ) ) );
		}
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return dialect.createTupleAssociation( associationKey, rowKey );
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return dialect.nextValue( request );
	}

	@Override
	public boolean supportsSequences() {
		return false;
	}

	@Override
	public GridType overrideType(Type type) {
		return dialect.overrideType( type );
	}

	@Override
	public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		dialect.forEachTuple( consumer, entityKeyMetadatas );
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
		return dialect.isStoredInEntityStructure( associationKey, associationContext );
	}

	private AssociationContext withQueue(AssociationContext associationContext) {
		return new AssociationContext(
				associationContext.getOptionsContext(),
				getOperationQueue()
		);
	}
}
