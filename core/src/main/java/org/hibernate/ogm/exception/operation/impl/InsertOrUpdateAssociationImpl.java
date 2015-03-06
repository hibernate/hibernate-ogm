/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.operation.impl;

import org.hibernate.ogm.exception.operation.spi.InsertOrUpdateAssociation;
import org.hibernate.ogm.exception.operation.spi.InsertOrUpdateTuple;
import org.hibernate.ogm.exception.operation.spi.OperationType;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.spi.Association;

/**
 * @author Gunnar Morling
 *
 */
public class InsertOrUpdateAssociationImpl extends AbstractGridDialectOperation implements InsertOrUpdateAssociation {

	private final AssociationKey associationKey;
	private final Association association;

	public InsertOrUpdateAssociationImpl(AssociationKey associationKey, Association association) {
		super( InsertOrUpdateTuple.class, OperationType.INSERT_OR_UPDATE_ASSOCIATION );
		this.associationKey = associationKey;
		this.association = association;
	}

	public AssociationKey getAssociationKey() {
		return associationKey;
	}

	public Association getAssociation() {
		return association;
	}

	@Override
	public String toString() {
		return "InsertOrUpdateAssociationImpl [associationKey=" + associationKey + ", association=" + association + "]";
	}
}
