/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.batch;

import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.grid.AssociationKey;

/**
 * An operation representing the removal of an association
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class RemoveAssociationOperation implements Operation {

	private final AssociationKey associationKey;
	private final AssociationContext context;

	public RemoveAssociationOperation(AssociationKey associationKey, AssociationContext context) {
		this.associationKey = associationKey;
		this.context = context;
	}

	public AssociationKey getAssociationKey() {
		return associationKey;
	}

	public AssociationContext getContext() {
		return context;
	}

}
