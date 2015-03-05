/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.operation.impl;

import org.hibernate.ogm.exception.operation.spi.OperationType;
import org.hibernate.ogm.exception.operation.spi.RemoveTuple;
import org.hibernate.ogm.model.key.spi.EntityKey;

/**
 * @author Gunnar Morling
 *
 */
public class RemoveTupleImpl extends AbstractGridDialectOperation implements RemoveTuple {

	private final EntityKey entityKey;

	public RemoveTupleImpl(EntityKey entityKey) {
		super( RemoveTuple.class, OperationType.REMOVE_TUPLE );
		this.entityKey = entityKey;
	}

	@Override
	public EntityKey getEntityKey() {
		return entityKey;
	}

	@Override
	public String toString() {
		return "RemoveTupleImpl [entityKey=" + entityKey + "]";
	}
}
