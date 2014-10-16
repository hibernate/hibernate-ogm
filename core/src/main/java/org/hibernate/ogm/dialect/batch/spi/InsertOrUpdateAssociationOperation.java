/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.batch.spi;

import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.spi.Association;

/**
 * Contains the data required to update an association
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class InsertOrUpdateAssociationOperation implements Operation {

	private final Association association;
	private final AssociationKey associationKey;
	private final AssociationContext context;

	public InsertOrUpdateAssociationOperation(Association association, AssociationKey associationKey, AssociationContext context) {
		this.association = association;
		this.associationKey = associationKey;
		this.context = context;
	}

	public Association getAssociation() {
		return association;
	}

	public AssociationKey getAssociationKey() {
		return associationKey;
	}

	public AssociationContext getContext() {
		return context;
	}

}
