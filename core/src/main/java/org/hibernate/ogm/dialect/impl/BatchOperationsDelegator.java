/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import java.io.Serializable;

import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.batch.spi.GroupingByEntityDialect;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateTupleOperation;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.batch.spi.RemoveAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.RemoveTupleOperation;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManager;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

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

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final EventContextManager eventContext;

	public BatchOperationsDelegator(GridDialect dialect, EventContextManager eventContext) {
		super( dialect );
		this.eventContext = eventContext;
	}

	private boolean isBatchDisabled() {
		return getOperationQueue().isClosed();
	}

	private OperationsQueue getOperationQueue() {
		OperationsQueue operationsQueue = null;

		if ( eventContext.isActive() ) {
			operationsQueue = eventContext.get( OperationsQueue.class );
		}

		if ( operationsQueue == null ) {
			return OperationsQueue.CLOSED_QUEUE;
		}
		else {
			return operationsQueue;
		}
	}

	@Override
	public void executeBatch(OperationsQueue operationsQueue) {
		try {
			if ( GridDialects.hasFacet( getGridDialect(), BatchableGridDialect.class )
					|| GridDialects.hasFacet( getGridDialect(), GroupingByEntityDialect.class ) ) {
				log.tracef( "Executing batch" );
				super.executeBatch( operationsQueue );
			}
		}
		catch ( TupleAlreadyExistsException taee ) {
			// TODO: Ideally, we should log the entity name + id here; For now we trust the datastore to provide this
			// information via the original exception; It'd require a fair bit of changes to obtain the entity name here
			// (we'd have to obtain the persister matching the given entity key metadata which in turn would require
			// access to the session factory which is not easily available here)
			throw log.mustNotInsertSameEntityTwice( taee.getMessage(), taee );
		}
	}

	@Override
	public Tuple getTuple(EntityKey key, OperationContext operationContext) {
		OperationContext contextWithQueue;
		if ( operationContext instanceof AssociationContext ) {
			contextWithQueue = new AssociationContextImpl( (AssociationContextImpl) operationContext, getOperationQueue() );
		}
		else {
			contextWithQueue = new TupleContextImpl( (TupleContextImpl) operationContext, getOperationQueue() );
		}

		return super.getTuple( key, contextWithQueue );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) {
		if ( isBatchDisabled() ) {
			super.insertOrUpdateTuple( key, tuplePointer, tupleContext );
		}
		else {
			getOperationQueue().add( new InsertOrUpdateTupleOperation( tuplePointer, key, tupleContext ) );
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

	@Override
	public void flushPendingOperations(EntityKey entityKey, TupleContext tupleContext) {
		super.flushPendingOperations( entityKey, withQueue( tupleContext ) );
	}

	private AssociationContext withQueue(AssociationContext associationContext) {
		return new AssociationContextImpl( (AssociationContextImpl) associationContext, getOperationQueue() );
	}

	private TupleContext withQueue(TupleContext tupleContext) {
		return new TupleContextImpl( (TupleContextImpl) tupleContext, getOperationQueue() );
	}
}
