/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.failure.operation.impl;

import org.hibernate.ogm.failure.operation.CreateAssociationWithKey;
import org.hibernate.ogm.failure.operation.GridDialectOperation;
import org.hibernate.ogm.failure.operation.OperationType;
import org.hibernate.ogm.model.key.spi.AssociationKey;

/**
 * @author Gunnar Morling
 *
 */
public class CreateAssociationWithKeyImpl implements CreateAssociationWithKey {

	private final AssociationKey associationKey;

	public CreateAssociationWithKeyImpl(AssociationKey associationKey) {
		this.associationKey = associationKey;
	}

	@Override
	public AssociationKey getAssociationKey() {
		return associationKey;
	}

	@Override
	public <T extends GridDialectOperation> T as(Class<T> type) {
		if ( CreateAssociationWithKey.class.isAssignableFrom( type ) ) {
			return type.cast( this );
		}

		throw new IllegalArgumentException( "Unexpected type: " + type );
	}

	@Override
	public OperationType getType() {
		return OperationType.CREATE_ASSOCIATION_WITH_KEY;
	}

	@Override
	public String toString() {
		return "CreateAssociationImpl [associationKey=" + associationKey + "]";
	}
}
