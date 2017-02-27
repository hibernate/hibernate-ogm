/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import org.hibernate.ogm.dialect.batch.spi.GroupedChangesToEntityOperation;
import org.hibernate.ogm.dialect.batch.spi.GroupingByEntityDialect;
import org.hibernate.ogm.dialect.batch.spi.Operation;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.batch.spi.RemoveTupleOperation;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;


/**
 * Base class of all {@link GridDialect}s implementing the grouping of operations per entity.
 * <p>
 * The idea is to group all the insert/update operations regarding the same entity in one datastore operation.
 *
 * @author Guillaume Smet
 */
public abstract class AbstractGroupingByEntityDialect extends BaseGridDialect implements GroupingByEntityDialect {

	@Override
	public void executeBatch(OperationsQueue queue) {
		if ( !queue.isClosed() ) {
			Operation operation = queue.poll();

			while ( operation != null ) {
				if ( operation instanceof GroupedChangesToEntityOperation ) {
					GroupedChangesToEntityOperation entityOperation = (GroupedChangesToEntityOperation) operation;
					executeGroupedChangesToEntity( entityOperation );
				}
				else if ( operation instanceof RemoveTupleOperation ) {
					RemoveTupleOperation removeTupleOperation = (RemoveTupleOperation) operation;
					removeTuple( removeTupleOperation.getEntityKey(), removeTupleOperation.getTupleContext() );
				}
				else {
					throw new UnsupportedOperationException( "Operation not supported: " + operation.getClass().getSimpleName() );
				}
				operation = queue.poll();
			}

			queue.clear();
		}
	}

	@Override
	public void flushPendingOperations(EntityKey entityKey, TupleContext tupleContext) {
		executeBatch( tupleContext.getOperationsQueue() );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) {
		throw new UnsupportedOperationException( "Method not supported by GroupingByEntityDialect implementations" );
	}

	@Override
	public void insertOrUpdateAssociation(
			AssociationKey associationKey, org.hibernate.ogm.model.spi.Association association,
			AssociationContext associationContext) {
		throw new UnsupportedOperationException( "Method not supported by GroupingByEntityDialect implementations" );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		throw new UnsupportedOperationException( "Method not supported by GroupingByEntityDialect implementations" );
	}

	protected abstract void executeGroupedChangesToEntity(GroupedChangesToEntityOperation groupedOperation);

}
