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
import org.hibernate.ogm.exception.operation.impl.CreateTupleImpl;
import org.hibernate.ogm.exception.operation.impl.CreateTupleWithKeyImpl;
import org.hibernate.ogm.exception.operation.impl.ExecuteBatchImpl;
import org.hibernate.ogm.exception.operation.impl.InsertOrUpdateTupleImpl;
import org.hibernate.ogm.exception.operation.impl.InsertTupleImpl;
import org.hibernate.ogm.exception.operation.impl.RemoveTupleImpl;
import org.hibernate.ogm.exception.operation.impl.UpdateTupleWithOptimisticLockImpl;
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
		invocationCollector.add( new InsertOrUpdateTupleImpl( key, tuple ) );
	}

	@Override
	public void executeBatch(OperationsQueue queue) {
		super.executeBatch( queue );
		invocationCollector.add( new ExecuteBatchImpl() );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		Tuple tuple = super.createTuple( key, tupleContext );
		invocationCollector.add( new CreateTupleWithKeyImpl( key ) );
		return tuple;
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		super.removeTuple( key, tupleContext );
		invocationCollector.add( new RemoveTupleImpl( key ) );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		// TODO Collect
		return super.createAssociation( key, associationContext );
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		// TODO Collect
		super.insertOrUpdateAssociation( key, association, associationContext );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		// TODO Collect
		super.removeAssociation( key, associationContext );
	}

	// IdentityColumnAwareGridDialect

	@Override
	public Tuple createTuple(EntityKeyMetadata entityKeyMetadata, TupleContext tupleContext) {
		Tuple tuple = super.createTuple( entityKeyMetadata, tupleContext );
		invocationCollector.add( new CreateTupleImpl( entityKeyMetadata ) );
		return tuple;
	}

	@Override
	public void insertTuple(EntityKeyMetadata entityKeyMetadata, Tuple tuple, TupleContext tupleContext) {
		super.insertTuple( entityKeyMetadata, tuple, tupleContext );
		invocationCollector.add( new InsertTupleImpl( entityKeyMetadata, tuple ) );
	}

	// OptimisticLockingAwareGridDialect

	@Override
	public boolean updateTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, Tuple tuple, TupleContext tupleContext) {
		boolean success = super.updateTupleWithOptimisticLock( entityKey, oldLockState, tuple, tupleContext );

		if ( success ) {
			invocationCollector.add( new UpdateTupleWithOptimisticLockImpl( entityKey, oldLockState, tuple ) );
		}

		return success;
	}

	@Override
	public boolean removeTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, TupleContext tupleContext) {
		// TODO Collect
		return super.removeTupleWithOptimisticLock( entityKey, oldLockState, tupleContext );
	}
}
