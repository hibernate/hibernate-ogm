/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.io.Serializable;

import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateTupleOperation;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.batch.spi.RemoveAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.RemoveTupleOperation;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;

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
public class BatchOperationsDelegator extends ForwardingGridDialect<Serializable> {

	// The threadlocal is properly set and cleaned in a try / catch by {@link org.hibernate.ogm.service.impl.BatchManagerEventListener}
	// if used elsewhere, apply the same pattern
	private final ThreadLocal<OperationsQueue> operationQueueLocal = new ThreadLocal<OperationsQueue>();

	public BatchOperationsDelegator(BatchableGridDialect dialect) {
		super( dialect );
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
		super.executeBatch( getOperationQueue() );
	}


	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		TupleContext contextWithQueue = new TupleContextImpl(
				(TupleContextImpl) tupleContext,
				getOperationQueue()
		);

		return super.getTuple( key, contextWithQueue );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		if ( isBatchDisabled() ) {
			super.insertOrUpdateTuple( key, tuple, tupleContext );
		}
		else {
			getOperationQueue().add( new InsertOrUpdateTupleOperation( tuple, key, tupleContext ) );
		}
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		if ( isBatchDisabled() ) {
			super.removeTuple( key, tupleContext );
		}
		else {
			getOperationQueue().add( new RemoveTupleOperation( key, tupleContext ) );
		}
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		return super.getAssociation( key, withQueue( associationContext ) );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		return super.createAssociation( key, withQueue( associationContext ) );
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		if ( isBatchDisabled() ) {
			super.insertOrUpdateAssociation( key, association, withQueue( associationContext ) );
		}
		else {
			getOperationQueue().add( new InsertOrUpdateAssociationOperation( association, key, withQueue( associationContext ) ) );
		}
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		if ( isBatchDisabled() ) {
			super.removeAssociation( key, withQueue( associationContext ) );
		}
		else {
			getOperationQueue().add( new RemoveAssociationOperation( key, withQueue( associationContext ) ) );
		}
	}

	private AssociationContext withQueue(AssociationContext associationContext) {
		return new AssociationContextImpl( (AssociationContextImpl) associationContext, getOperationQueue() );
	}
}
