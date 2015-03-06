/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.operation.impl;

import org.hibernate.ogm.exception.operation.spi.CreateAssociationWithKey;
import org.hibernate.ogm.exception.operation.spi.CreateTuple;
import org.hibernate.ogm.exception.operation.spi.OperationType;
import org.hibernate.ogm.model.key.spi.AssociationKey;

/**
 * @author Gunnar Morling
 *
 */
public class CreateAssociationWithKeyImpl extends AbstractGridDialectOperation implements CreateAssociationWithKey {

	private final AssociationKey associationKey;

	public CreateAssociationWithKeyImpl(AssociationKey associationKey) {
		super( CreateTuple.class, OperationType.CREATE_ASSOCIATION_WITH_KEY );
		this.associationKey = associationKey;
	}

	@Override
	public AssociationKey getAssociationKey() {
		return associationKey;
	}

	@Override
	public String toString() {
		return "CreateAssociationImpl [associationKey=" + associationKey + "]";
	}
}
