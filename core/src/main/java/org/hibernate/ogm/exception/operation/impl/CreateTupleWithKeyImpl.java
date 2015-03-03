/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.operation.impl;

import org.hibernate.ogm.exception.operation.spi.CreateTupleWithKey;
import org.hibernate.ogm.exception.operation.spi.OperationType;
import org.hibernate.ogm.model.key.spi.EntityKey;

/**
 * @author Gunnar Morling
 *
 */
public class CreateTupleWithKeyImpl extends AbstractGridDialectOperation implements CreateTupleWithKey {

	private final EntityKey entityKey;

	public CreateTupleWithKeyImpl(EntityKey entityKey) {
		super( CreateTupleWithKey.class, OperationType.CREATE_TUPLE_WITH_KEY );
		this.entityKey = entityKey;
	}

	@Override
	public EntityKey getEntityKey() {
		return entityKey;
	}

	@Override
	public String toString() {
		return "CreateTupleWithKeyImpl [entityKey=" + entityKey + "]";
	}
}
