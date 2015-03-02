/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.operation.impl;

import org.hibernate.ogm.exception.operation.spi.CreateTuple;
import org.hibernate.ogm.exception.operation.spi.OperationType;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;

/**
 * @author Gunnar Morling
 *
 */
public class CreateTupleImpl extends AbstractGridDialectOperation implements CreateTuple {

	private final EntityKeyMetadata entityKeyMetadata;

	public CreateTupleImpl(EntityKeyMetadata entityKeyMetadata) {
		super( CreateTuple.class, OperationType.CREATE_TUPLE );
		this.entityKeyMetadata = entityKeyMetadata;
	}

	@Override
	public EntityKeyMetadata getEntityKeyMetadata() {
		return entityKeyMetadata;
	}

	@Override
	public String toString() {
		return "CreateTupleImpl [entityKeyMetadata=" + entityKeyMetadata + "]";
	}
}
