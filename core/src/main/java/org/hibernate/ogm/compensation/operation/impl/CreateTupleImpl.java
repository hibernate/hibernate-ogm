/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation.operation.impl;

import org.hibernate.ogm.compensation.operation.CreateTuple;
import org.hibernate.ogm.compensation.operation.GridDialectOperation;
import org.hibernate.ogm.compensation.operation.OperationType;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;

/**
 * @author Gunnar Morling
 *
 */
public class CreateTupleImpl implements CreateTuple {

	private final EntityKeyMetadata entityKeyMetadata;

	public CreateTupleImpl(EntityKeyMetadata entityKeyMetadata) {
		this.entityKeyMetadata = entityKeyMetadata;
	}

	@Override
	public EntityKeyMetadata getEntityKeyMetadata() {
		return entityKeyMetadata;
	}


	@Override
	public <T extends GridDialectOperation> T as(Class<T> type) {
		if ( CreateTuple.class.isAssignableFrom( type ) ) {
			return type.cast( this );
		}

		throw new IllegalArgumentException( "Unexpected type: " + type );
	}

	@Override
	public OperationType getType() {
		return OperationType.CREATE_TUPLE;
	}

	@Override
	public String toString() {
		return "CreateTupleImpl [entityKeyMetadata=" + entityKeyMetadata + "]";
	}
}
