/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.ogm.compensation.ErrorHandler;
import org.hibernate.ogm.compensation.ErrorHandlingStrategy;
import org.hibernate.ogm.compensation.operation.CreateAssociationWithKey;
import org.hibernate.ogm.compensation.operation.CreateTuple;
import org.hibernate.ogm.compensation.operation.CreateTupleWithKey;
import org.hibernate.ogm.compensation.operation.ExecuteBatch;
import org.hibernate.ogm.compensation.operation.FlushPendingOperations;
import org.hibernate.ogm.compensation.operation.GridDialectOperation;
import org.hibernate.ogm.compensation.operation.InsertOrUpdateAssociation;
import org.hibernate.ogm.compensation.operation.InsertTuple;
import org.hibernate.ogm.compensation.operation.RemoveAssociation;
import org.hibernate.ogm.compensation.operation.RemoveTuple;
import org.hibernate.ogm.compensation.operation.UpdateTupleWithOptimisticLock;
import org.hibernate.ogm.compensation.operation.impl.CreateAssociationWithKeyImpl;
import org.hibernate.ogm.compensation.operation.impl.CreateTupleImpl;
import org.hibernate.ogm.compensation.operation.impl.CreateTupleWithKeyImpl;
import org.hibernate.ogm.compensation.operation.impl.ExecuteBatchImpl;
import org.hibernate.ogm.compensation.operation.impl.FlushPendingOperationsImpl;
import org.hibernate.ogm.compensation.operation.impl.InsertOrUpdateAssociationImpl;
import org.hibernate.ogm.compensation.operation.impl.InsertOrUpdateTupleImpl;
import org.hibernate.ogm.compensation.operation.impl.InsertTupleImpl;
import org.hibernate.ogm.compensation.operation.impl.RemoveAssociationImpl;
import org.hibernate.ogm.compensation.operation.impl.RemoveTupleImpl;
import org.hibernate.ogm.compensation.operation.impl.RemoveTupleWithOptimisticLockImpl;
import org.hibernate.ogm.compensation.operation.impl.UpdateTupleWithOptimisticLockImpl;
import org.hibernate.ogm.dialect.batch.spi.GroupedChangesToEntityOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateTupleOperation;
import org.hibernate.ogm.dialect.batch.spi.Operation;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.batch.spi.RemoveAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.RemoveTupleOperation;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManager;
import org.hibernate.ogm.dialect.impl.ForwardingGridDialect;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.exception.impl.Exceptions;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * A grid dialect which tracks all applied and failing operations and passes them on to the {@link OperationCollector}
 * which in turn makes them available to the registered {@link ErrorHandler}.
 *
 * @author Gunnar Morling
 */
public class InvocationCollectingGridDialect extends ForwardingGridDialect<Serializable> {

	private final EventContextManager eventContext;

	public InvocationCollectingGridDialect(GridDialect gridDialect, EventContextManager eventContextManager) {
		super( gridDialect );
		this.eventContext = eventContextManager;
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) {
		InsertOrUpdateTupleImpl insertOrUpdateTuple = new InsertOrUpdateTupleImpl( key, tuplePointer.getTuple() );

		try {
			super.insertOrUpdateTuple( key, tuplePointer, tupleContext );
		}
		catch (Exception e) {
			handleException( insertOrUpdateTuple, e );
		}

		handleAppliedOperation( insertOrUpdateTuple );
	}

	public void onInsertOrUpdateTupleFailure(EntityKey key, Tuple tuple, TupleAlreadyExistsException e) {
		handleException( new InsertOrUpdateTupleImpl( key, tuple ), e );
	}

	@Override
	public void executeBatch(OperationsQueue queue) {
		OperationsQueue newQueue = new OperationsQueue();
		List<GridDialectOperation> operations = new ArrayList<>();

		if ( !queue.isClosed() ) {
			Operation operation = queue.poll();

			// TODO OGM-766 Avoid the looping + re-creation
			while ( operation != null ) {
				newQueue.add( operation );

				if ( operation instanceof GroupedChangesToEntityOperation ) {
					GroupedChangesToEntityOperation groupedChangesOnEntity = (GroupedChangesToEntityOperation) operation;
					for ( Operation groupedOperation : groupedChangesOnEntity.getOperations() ) {
						operations.add( getSimpleGridDialectOperations( groupedOperation ) );
					}
				}
				else {
					operations.add( getSimpleGridDialectOperations( operation ) );
				}

				operation = queue.poll();
			}
		}

		ExecuteBatch executeBatch = new ExecuteBatchImpl( operations );
		try {
			super.executeBatch( newQueue );
		}
		catch (Exception e) {
			handleException( executeBatch, e );
		}

		handleAppliedOperation( executeBatch );
	}

	@Override
	public void flushPendingOperations(EntityKey entityKey, TupleContext tupleContext) {
		OperationsQueue queue = tupleContext.getOperationsQueue();
		OperationsQueue newQueue = new OperationsQueue();
		List<GridDialectOperation> operations = new ArrayList<>();

		if ( !queue.isClosed() ) {
			Operation operation = queue.poll();

			// TODO OGM-766 Avoid the looping + re-creation
			while ( operation != null ) {
				newQueue.add( operation );

				if ( operation instanceof GroupedChangesToEntityOperation ) {
					GroupedChangesToEntityOperation groupedChangesOnEntity = (GroupedChangesToEntityOperation) operation;
					for ( Operation groupedOperation : groupedChangesOnEntity.getOperations() ) {
						operations.add( getSimpleGridDialectOperations( groupedOperation ) );
					}
				}
				else {
					operations.add( getSimpleGridDialectOperations( operation ) );
				}

				operation = queue.poll();
			}
		}

		FlushPendingOperations flushPendingOperations = new FlushPendingOperationsImpl( operations );
		try {
			super.flushPendingOperations( entityKey, tupleContext );
		}
		catch (Exception e) {
			handleException( flushPendingOperations, e );
		}

		handleAppliedOperation( flushPendingOperations );
	}

	private GridDialectOperation getSimpleGridDialectOperations(Operation operation) {
		GridDialectOperation gridDialectOperation;
		if ( operation instanceof InsertOrUpdateTupleOperation ) {
			InsertOrUpdateTupleOperation insertOrUpdateTuple = (InsertOrUpdateTupleOperation) operation;
			gridDialectOperation = new InsertOrUpdateTupleImpl( insertOrUpdateTuple.getEntityKey(), insertOrUpdateTuple.getTuplePointer().getTuple() );
		}
		else if ( operation instanceof RemoveTupleOperation ) {
			RemoveTupleOperation removeTuple = (RemoveTupleOperation) operation;
			gridDialectOperation = new RemoveTupleImpl( removeTuple.getEntityKey() );
		}
		else if ( operation instanceof InsertOrUpdateAssociationOperation ) {
			InsertOrUpdateAssociationOperation insertOrUpdateAssociationOperation = (InsertOrUpdateAssociationOperation) operation;
			gridDialectOperation = new InsertOrUpdateAssociationImpl(
					insertOrUpdateAssociationOperation.getAssociationKey(),
					insertOrUpdateAssociationOperation.getAssociation() );
		}
		else if ( operation instanceof RemoveAssociationOperation ) {
			RemoveAssociationOperation removeAssociationOperation = (RemoveAssociationOperation) operation;
			gridDialectOperation = new RemoveAssociationImpl( removeAssociationOperation.getAssociationKey() );
		}
		else {
			throw new IllegalStateException( "Unsupported operation " + operation );
		}
		return gridDialectOperation;
	}

	@Override
	public Tuple createTuple(EntityKey key, OperationContext operationContext) {
		Tuple tuple = null;
		CreateTupleWithKey createTupleWithKey = new CreateTupleWithKeyImpl( key );

		try {
			tuple = super.createTuple( key, operationContext );
		}
		catch (Exception e) {
			handleException( createTupleWithKey, e );
		}

		handleAppliedOperation( createTupleWithKey );
		return tuple;
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		RemoveTuple removeTuple = new RemoveTupleImpl( key );

		try {
			super.removeTuple( key, tupleContext );
		}
		catch (Exception e) {
			handleException( removeTuple, e );
		}

		handleAppliedOperation( removeTuple );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		Association association = null;
		CreateAssociationWithKey createAssociationWithKey = new CreateAssociationWithKeyImpl( key );

		try {
			association = super.createAssociation( key, associationContext );
		}
		catch (Exception e) {
			handleException( createAssociationWithKey, e );
		}

		handleAppliedOperation( createAssociationWithKey );
		return association;
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		InsertOrUpdateAssociation insertOrUpdateAssociation = new InsertOrUpdateAssociationImpl( key, association );

		try {
			super.insertOrUpdateAssociation( key, association, associationContext );
		}
		catch (Exception e) {
			handleException( insertOrUpdateAssociation, e );
		}

		handleAppliedOperation( insertOrUpdateAssociation );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		RemoveAssociation removeAssociation = new RemoveAssociationImpl( key );

		try {
			super.removeAssociation( key, associationContext );
		}
		catch (Exception e) {
			handleException( removeAssociation, e );
		}

		handleAppliedOperation( removeAssociation );
	}

	// IdentityColumnAwareGridDialect

	@Override
	public Tuple createTuple(EntityKeyMetadata entityKeyMetadata, OperationContext operationContext) {
		Tuple tuple = null;
		CreateTuple createTuple = new CreateTupleImpl( entityKeyMetadata );

		try {
			tuple = super.createTuple( entityKeyMetadata, operationContext );
		}
		catch (Exception e) {
			handleException( createTuple, e );
		}

		handleAppliedOperation( createTuple );
		return tuple;
	}

	@Override
	public void insertTuple(EntityKeyMetadata entityKeyMetadata, Tuple tuple, TupleContext tupleContext) {
		InsertTuple insertTuple = new InsertTupleImpl( entityKeyMetadata, tuple );

		try {
			super.insertTuple( entityKeyMetadata, tuple, tupleContext );
		}
		catch (Exception e) {
			handleException( insertTuple, e );
		}

		handleAppliedOperation( insertTuple );
	}

	// OptimisticLockingAwareGridDialect

	@Override
	public boolean updateTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, Tuple tuple, TupleContext tupleContext) {
		UpdateTupleWithOptimisticLock updateTupleWithOptimisticLock = new UpdateTupleWithOptimisticLockImpl( entityKey, oldLockState, tuple );
		boolean success = false;

		try {
			success = super.updateTupleWithOptimisticLock( entityKey, oldLockState, tuple, tupleContext );
		}
		catch (Exception e) {
			handleException( updateTupleWithOptimisticLock, e );
		}

		// applied/failed operation logging triggered by persister as per operation outcome

		return success;
	}

	public void onUpdateTupleWithOptimisticLockSuccess(EntityKey entityKey, Tuple oldLockState, Tuple tuple) {
		handleAppliedOperation( new UpdateTupleWithOptimisticLockImpl( entityKey, oldLockState, tuple ) );
	}

	public void onUpdateTupleWithOptimisticLockFailure(EntityKey entityKey, Tuple oldLockState, Tuple tuple, Exception e) {
		handleException( new UpdateTupleWithOptimisticLockImpl( entityKey, oldLockState, tuple ), e );
	}

	@Override
	public boolean removeTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, TupleContext tupleContext) {
		RemoveTupleWithOptimisticLockImpl removeTupleWithOptimisticLock = new RemoveTupleWithOptimisticLockImpl( entityKey, oldLockState );
		boolean success = false;

		try {
			success = super.removeTupleWithOptimisticLock( entityKey, oldLockState, tupleContext );
		}
		catch (Exception e) {
			handleException( removeTupleWithOptimisticLock, e );
		}

		// applied/failed operation logging triggered by persister as per operation outcome

		return success;
	}

	public void onRemoveTupleWithOptimisticLockSuccess(EntityKey entityKey, Tuple oldLockState) {
		handleAppliedOperation( new RemoveTupleWithOptimisticLockImpl( entityKey, oldLockState ) );
	}

	public void onRemoveTupleWithOptimisticLockFailure(EntityKey entityKey, Tuple oldLockState, Exception e) {
		handleException( new RemoveTupleWithOptimisticLockImpl( entityKey, oldLockState ), e );
	}

	private void handleAppliedOperation(GridDialectOperation operation) {
		getOperationCollector().addAppliedOperation( operation );
	}

	private void handleException(GridDialectOperation operation, Exception e) {
		ErrorHandlingStrategy result = getOperationCollector().onFailedOperation( operation, e );

		if ( result == ErrorHandlingStrategy.ABORT ) {
			Exceptions.<RuntimeException>sneakyThrow( e );
		}
	}

	/**
	 * Returns the {@link OperationCollector}. Must not store this as a field as it is flush-cycle scoped!
	 */
	private OperationCollector getOperationCollector() {
		return eventContext.get( OperationCollector.class );
	}
}
