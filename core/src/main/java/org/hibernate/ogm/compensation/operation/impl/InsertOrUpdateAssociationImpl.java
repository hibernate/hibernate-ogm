/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation.operation.impl;

import org.hibernate.ogm.compensation.operation.GridDialectOperation;
import org.hibernate.ogm.compensation.operation.InsertOrUpdateAssociation;
import org.hibernate.ogm.compensation.operation.OperationType;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.spi.Association;

/**
 * @author Gunnar Morling
 *
 */
public class InsertOrUpdateAssociationImpl implements InsertOrUpdateAssociation {

	private final AssociationKey associationKey;
	private final Association association;

	public InsertOrUpdateAssociationImpl(AssociationKey associationKey, Association association) {
		this.associationKey = associationKey;
		this.association = association;
	}

	@Override
	public AssociationKey getAssociationKey() {
		return associationKey;
	}

	@Override
	public Association getAssociation() {
		return association;
	}

	@Override
	public <T extends GridDialectOperation> T as(Class<T> type) {
		if ( InsertOrUpdateAssociation.class.isAssignableFrom( type ) ) {
			return type.cast( this );
		}

		throw new IllegalArgumentException( "Unexpected type: " + type );
	}

	@Override
	public OperationType getType() {
		return OperationType.INSERT_OR_UPDATE_ASSOCIATION;
	}

	@Override
	public String toString() {
		return "InsertOrUpdateAssociationImpl [associationKey=" + associationKey + ", association=" + association + "]";
	}
}
