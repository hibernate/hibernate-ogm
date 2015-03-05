/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.impl;

import java.io.Serializable;

import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.impl.ForwardingGridDialect;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
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
import org.hibernate.ogm.exception.operation.spi.GridDialectOperation;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * @author Gunnar Morling
 */
public class InvocationCollectingGridDialect extends ForwardingGridDialect<Serializable> {

	private final GridDialectInvocationCollector invocationCollector;

	public InvocationCollectingGridDialect(GridDialect gridDialect, GridDialectInvocationCollector invocationCollector) {
		super( gridDialect );
		this.invocationCollector = invocationCollector;
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		super.insertOrUpdateTuple( key, tuple, tupleContext );
		handleAppliedOperation( new InsertOrUpdateTupleImpl( key, tuple ) );
	}

	@Override
	public void executeBatch(OperationsQueue queue) {
		super.executeBatch( queue );
		handleAppliedOperation( new ExecuteBatchImpl() );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		Tuple tuple = super.createTuple( key, tupleContext );
		handleAppliedOperation( new CreateTupleWithKeyImpl( key ) );
		return tuple;
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		super.removeTuple( key, tupleContext );
		handleAppliedOperation( new RemoveTupleImpl( key ) );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		Association association = super.createAssociation( key, associationContext );
		handleAppliedOperation( new CreateAssociationWithKeyImpl( key ) );
		return association;
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		super.insertOrUpdateAssociation( key, association, associationContext );
		handleAppliedOperation( new InsertOrUpdateAssociationImpl( key, association ) );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		super.removeAssociation( key, associationContext );
		handleAppliedOperation( new RemoveAssociationImpl( key ) );
	}

	// IdentityColumnAwareGridDialect

	@Override
	public Tuple createTuple(EntityKeyMetadata entityKeyMetadata, TupleContext tupleContext) {
		Tuple tuple = super.createTuple( entityKeyMetadata, tupleContext );
		handleAppliedOperation( new CreateTupleImpl( entityKeyMetadata ) );
		return tuple;
	}

	@Override
	public void insertTuple(EntityKeyMetadata entityKeyMetadata, Tuple tuple, TupleContext tupleContext) {
		super.insertTuple( entityKeyMetadata, tuple, tupleContext );
		handleAppliedOperation( new InsertTupleImpl( entityKeyMetadata, tuple ) );
	}

	// OptimisticLockingAwareGridDialect

	@Override
	public boolean updateTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, Tuple tuple, TupleContext tupleContext) {
		boolean success = super.updateTupleWithOptimisticLock( entityKey, oldLockState, tuple, tupleContext );

		if ( success ) {
			handleAppliedOperation( new UpdateTupleWithOptimisticLockImpl( entityKey, oldLockState, tuple ) );
		}

		return success;
	}

	@Override
	public boolean removeTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, TupleContext tupleContext) {
		boolean success = super.removeTupleWithOptimisticLock( entityKey, oldLockState, tupleContext );

		if ( success ) {
			handleAppliedOperation( new RemoveTupleWithOptimisticLockImpl( entityKey, oldLockState ) );
		}

		return success;
	}

	private void handleAppliedOperation(GridDialectOperation operation) {
		invocationCollector.add( operation );
	}
}
