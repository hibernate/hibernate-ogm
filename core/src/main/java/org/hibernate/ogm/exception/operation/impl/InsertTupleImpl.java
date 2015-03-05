/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.operation.impl;

import org.hibernate.ogm.exception.operation.spi.InsertTuple;
import org.hibernate.ogm.exception.operation.spi.OperationType;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * @author Gunnar Morling
 */
public class InsertTupleImpl extends AbstractGridDialectOperation implements InsertTuple {

	private final EntityKeyMetadata entityKeyMetadata;
	private final Tuple tuple;

	public InsertTupleImpl(EntityKeyMetadata entityKeyMetadata, Tuple tuple) {
		super( InsertTuple.class, OperationType.INSERT_TUPLE );
		this.entityKeyMetadata = entityKeyMetadata;
		this.tuple = tuple;
	}

	@Override
	public EntityKeyMetadata getEntityKeyMetadata() {
		return entityKeyMetadata;
	}

	@Override
	public Tuple getTuple() {
		return tuple;
	}

	@Override
	public String toString() {
		return "InsertTupleImpl [entityKeyMetadata=" + entityKeyMetadata + ", tuple=" + tuple + "]";
	}
}
