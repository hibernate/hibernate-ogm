/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation.operation.impl;

import org.hibernate.ogm.compensation.operation.GridDialectOperation;
import org.hibernate.ogm.compensation.operation.OperationType;
import org.hibernate.ogm.compensation.operation.RemoveTupleWithOptimisticLock;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * @author Gunnar Morling
 */
public class RemoveTupleWithOptimisticLockImpl implements RemoveTupleWithOptimisticLock {

	private final EntityKey entityKey;
	private final Tuple oldLockState;

	public RemoveTupleWithOptimisticLockImpl(EntityKey entityKey, Tuple oldLockState) {
		this.entityKey = entityKey;
		this.oldLockState = oldLockState;
	}

	@Override
	public EntityKey getEntityKey() {
		return entityKey;
	}

	@Override
	public Tuple getOldLockState() {
		return oldLockState;
	}

	@Override
	public <T extends GridDialectOperation> T as(Class<T> type) {
		if ( RemoveTupleWithOptimisticLock.class.isAssignableFrom( type ) ) {
			return type.cast( this );
		}

		throw new IllegalArgumentException( "Unexpected type: " + type );
	}

	@Override
	public OperationType getType() {
		return OperationType.REMOVE_TUPLE_WITH_OPTIMISTIC_LOCK;
	}

	@Override
	public String toString() {
		return "RemoveTupleWithOptimisticLockImpl [entityKey=" + entityKey + ", oldLockState=" + oldLockState + "]";
	}
}
