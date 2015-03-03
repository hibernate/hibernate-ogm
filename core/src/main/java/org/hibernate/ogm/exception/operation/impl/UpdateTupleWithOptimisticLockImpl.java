/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.operation.impl;

import org.hibernate.ogm.exception.operation.spi.OperationType;
import org.hibernate.ogm.exception.operation.spi.UpdateTupleWithOptimisticLock;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * @author Gunnar Morling
 */
public class UpdateTupleWithOptimisticLockImpl extends AbstractGridDialectOperation implements UpdateTupleWithOptimisticLock {

	private final EntityKey entityKey;
	private final Tuple oldLockState;
	private final Tuple tuple;

	public UpdateTupleWithOptimisticLockImpl(EntityKey entityKey, Tuple oldLockState, Tuple tuple) {
		super( UpdateTupleWithOptimisticLock.class, OperationType.UPDATE_TUPLE_WITH_OPTIMISTIC_LOCK );
		this.entityKey = entityKey;
		this.oldLockState = oldLockState;
		this.tuple = tuple;
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
	public Tuple getTuple() {
		return tuple;
	}

	@Override
	public String toString() {
		return "UpdateTupleWithOptimisticLockImpl [entityKey=" + entityKey + ", oldLockState=" + oldLockState + ", tuple=" + tuple + "]";
	}
}
