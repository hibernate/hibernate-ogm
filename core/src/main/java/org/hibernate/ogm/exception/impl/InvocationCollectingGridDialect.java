/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateTupleOperation;
import org.hibernate.ogm.dialect.batch.spi.Operation;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.batch.spi.RemoveAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.RemoveTupleOperation;
import org.hibernate.ogm.dialect.flushstate.impl.FlushCycleStateManager;
import org.hibernate.ogm.dialect.impl.BatchOperationsDelegator;
import org.hibernate.ogm.dialect.impl.ForwardingGridDialect;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.exception.operation.impl.CreateAssociationWithKeyImpl;
import org.hibernate.ogm.exception.operation.impl.CreateTupleImpl;
import org.hibernate.ogm.exception.operation.impl.CreateTupleWithKeyImpl;
import org.hibernate.ogm.exception.operation.impl.ExecuteBatchImpl;
import org.hibernate.ogm.exception.operation.impl.InsertOrUpdateAssociationImpl;
import org.hibernate.ogm.exception.operation.impl.InsertOrUpdateTupleImpl;
import org.hibernate.ogm.exception.operation.impl.InsertTupleImpl;
import org.hibernate.ogm.exception.operation.impl.RemoveAssociationImpl;
import org.hibernate.ogm.exception.operation.impl.RemoveTupleImpl;
import org.hibernate.ogm.exception.operation.impl.RemoveTupleWithOptimisticLockImpl;
import org.hibernate.ogm.exception.operation.impl.UpdateTupleWithOptimisticLockImpl;
import org.hibernate.ogm.exception.operation.spi.CreateAssociationWithKey;
import org.hibernate.ogm.exception.operation.spi.CreateTuple;
import org.hibernate.ogm.exception.operation.spi.CreateTupleWithKey;
import org.hibernate.ogm.exception.operation.spi.ExecuteBatch;
import org.hibernate.ogm.exception.operation.spi.GridDialectOperation;
import org.hibernate.ogm.exception.operation.spi.InsertOrUpdateAssociation;
import org.hibernate.ogm.exception.operation.spi.InsertTuple;
import org.hibernate.ogm.exception.operation.spi.RemoveAssociation;
import org.hibernate.ogm.exception.operation.spi.RemoveTuple;
import org.hibernate.ogm.exception.operation.spi.UpdateTupleWithOptimisticLock;
import org.hibernate.ogm.exception.spi.ErrorHandlingStrategy;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * @author Gunnar Morling
 */
public class InvocationCollectingGridDialect extends ForwardingGridDialect<Serializable> {

	private static final Log LOG = LoggerFactory.make();

	private final FlushCycleStateManager flushCycleStateManager;

	public InvocationCollectingGridDialect(GridDialect gridDialect, FlushCycleStateManager flushCycleStateManager) {
		super( gridDialect );
		this.flushCycleStateManager = flushCycleStateManager;
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		InsertOrUpdateTupleImpl insertOrUpdateTuple = new InsertOrUpdateTupleImpl( key, tuple );

		try {
			doInsertOrUpdateTuple( key, tuple, tupleContext );
		}
		catch (Exception e) {
			handleException( insertOrUpdateTuple, e );
		}

		handleAppliedOperation( insertOrUpdateTuple );
	}

	private void doInsertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		try {
			super.insertOrUpdateTuple( key, tuple, tupleContext );
		}
		catch ( TupleAlreadyExistsException taee ) {
			// TODO resemble OgmEntityPersister insert ?
			throw LOG.mustNotInsertSameEntityTwice( null, taee );
		}
	}

	@Override
	public void executeBatch(OperationsQueue queue) {
		OperationsQueue newQueue = new OperationsQueue();
		List<GridDialectOperation> operations = new ArrayList<>();

		if ( !queue.isClosed() ) {
			Operation operation = queue.poll();
			while ( operation != null ) {
				newQueue.add( operation );

				if ( operation instanceof InsertOrUpdateTupleOperation ) {
					InsertOrUpdateTupleOperation insertOrUpdateTuple = (InsertOrUpdateTupleOperation) operation;
					operations.add( new InsertOrUpdateTupleImpl( insertOrUpdateTuple.getEntityKey(), insertOrUpdateTuple.getTuple() ) );
				}
				else if ( operation instanceof RemoveTupleOperation ) {
					RemoveTupleOperation removeTuple = (RemoveTupleOperation) operation;
					operations.add( new RemoveTupleImpl( removeTuple.getEntityKey() ) );
				}
				else if ( operation instanceof InsertOrUpdateAssociationOperation ) {
					InsertOrUpdateAssociationOperation insertOrUpdateAssociationOperation = (InsertOrUpdateAssociationOperation) operation;
					operations.add( new InsertOrUpdateAssociationImpl(
							insertOrUpdateAssociationOperation.getAssociationKey(),
							insertOrUpdateAssociationOperation.getAssociation() )
					);
				}
				else if ( operation instanceof RemoveAssociationOperation ) {
					RemoveAssociationOperation removeAssociationOperation = (RemoveAssociationOperation) operation;
					operations.add( new RemoveAssociationImpl( removeAssociationOperation.getAssociationKey() ) );
				}

				operation = queue.poll();
			}
		}

		ExecuteBatch executeBatch = new ExecuteBatchImpl( operations );
		try {
			doExecuteBatch( newQueue );
		}
		catch (Exception e) {
			handleException( executeBatch, e );
		}

		handleAppliedOperation( executeBatch );
	}

	/**
	 * This resembles the exception translation done in {@link BatchOperationsDelegator#executeBatch()}. Need it here already
	 * in order to present the correct exception type to the error handler.
	 */
	private void doExecuteBatch(OperationsQueue queue) {
		try {
			super.executeBatch( queue );
		}
		catch ( TupleAlreadyExistsException taee ) {
			// TODO: Ideally, we should log the entity name + id here; For now we trust the datastore to provide this
			// information via the original exception; It'd require a fair bit of changes to obtain the entity name here
			// (we'd have to obtain the persister matching the given entity key metadata which in turn would require
			// access to the session factory which is not easily available here)
			throw LOG.mustNotInsertSameEntityTwice( null, taee );
		}
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		Tuple tuple = null;
		CreateTupleWithKey createTupleWithKey = new CreateTupleWithKeyImpl( key );

		try {
			tuple = super.createTuple( key, tupleContext );
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
	public Tuple createTuple(EntityKeyMetadata entityKeyMetadata, TupleContext tupleContext) {
		Tuple tuple = null;
		CreateTuple createTuple = new CreateTupleImpl( entityKeyMetadata );

		try {
			tuple = super.createTuple( entityKeyMetadata, tupleContext );
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

		// applied operation logging done in persister

		return success;
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

		// applied operation logging done in persister

		return success;
	}

	private void handleAppliedOperation(GridDialectOperation operation) {
		flushCycleStateManager.get( ErrorHandlerManager.class ).onAppliedOperation( operation );
	}

	private void handleException(GridDialectOperation operation, Exception e) {
		ErrorHandlingStrategy result = flushCycleStateManager.get( ErrorHandlerManager.class ).onFailedOperation( operation, e );

		if ( result == ErrorHandlingStrategy.ABORT ) {
			this.<RuntimeException>sneakyThrow( e );
		}
	}

	@SuppressWarnings("unchecked")
	private <E extends Exception> void sneakyThrow(Exception e) throws E {
		throw (E) e;
	}
}
