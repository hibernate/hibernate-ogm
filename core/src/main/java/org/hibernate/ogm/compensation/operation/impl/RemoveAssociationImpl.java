/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation.operation.impl;

import org.hibernate.ogm.compensation.operation.GridDialectOperation;
import org.hibernate.ogm.compensation.operation.OperationType;
import org.hibernate.ogm.compensation.operation.RemoveAssociation;
import org.hibernate.ogm.model.key.spi.AssociationKey;

/**
 * @author Gunnar Morling
 *
 */
public class RemoveAssociationImpl implements RemoveAssociation {

	private final AssociationKey associationKey;

	public RemoveAssociationImpl(AssociationKey associationKey) {
		this.associationKey = associationKey;
	}

	@Override
	public AssociationKey getAssociationKey() {
		return associationKey;
	}

	@Override
	public <T extends GridDialectOperation> T as(Class<T> type) {
		if ( RemoveAssociation.class.isAssignableFrom( type ) ) {
			return type.cast( this );
		}

		throw new IllegalArgumentException( "Unexpected type: " + type );
	}

	@Override
	public OperationType getType() {
		return OperationType.REMOVE_ASSOCIATION;
	}

	@Override
	public String toString() {
		return "RemoveAssociationImpl [associationKey=" + associationKey + "]";
	}
}
