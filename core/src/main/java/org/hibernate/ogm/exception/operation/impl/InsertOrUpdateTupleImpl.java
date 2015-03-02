/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.operation.impl;

import org.hibernate.ogm.exception.operation.spi.InsertOrUpdateTuple;
import org.hibernate.ogm.exception.operation.spi.OperationType;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * @author Gunnar Morling
 *
 */
public class InsertOrUpdateTupleImpl extends AbstractGridDialectOperation implements InsertOrUpdateTuple {

	private final EntityKey entityKey;
	private final Tuple tuple;

	public InsertOrUpdateTupleImpl(EntityKey entityKey, Tuple tuple) {
		super( InsertOrUpdateTuple.class, OperationType.INSERT_OR_UPDATE_TUPLE );
		this.entityKey = entityKey;
		this.tuple = tuple;
	}

	@Override
	public EntityKey getEntityKey() {
		return entityKey;
	}

	@Override
	public Tuple getTuple() {
		return tuple;
	}

	@Override
	public String toString() {
		return "InsertOrUpdateTupleImpl [entityKey=" + entityKey + ", tuple=" + tuple + "]";
	}
}
