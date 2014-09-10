/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.association.spi;

import org.hibernate.ogm.model.key.spi.AssociationKey;

/**
 * Contract for factories creating {@link AssociationRow} objects.
 *
 * @author Gunnar Morling
 */
public interface AssociationRowFactory {

	/**
	 * Creates an association row.
	 *
	 * @param associationKey The key of the association owning the given row. Will be used as source for values present
	 * in the key.
	 * @param row The association row in a store-specific representation
	 * @return An association row providing access to the values of the given association key and row
	 */
	AssociationRow<?> createAssociationRow(AssociationKey associationKey, Object row);
}
