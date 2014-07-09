/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.options.navigation;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.options.navigation.PropertyContext;

/**
 * Allows to configure document store options applying on a per-association level.
 *
 * @author Gunnar Morling
 */
public interface DocumentStorePropertyContext<E extends DocumentStoreEntityContext<E, P>, P extends DocumentStorePropertyContext<E, P>> extends
		PropertyContext<E, P> {

	/**
	 * Specifies how associations of the configured property should be persisted. Only applies if the property
	 * represents an association.
	 *
	 * @param storage the association storage type to be used; overrides any settings on the entity or global level
	 * @return this context, allowing for further fluent API invocations
	 */
	P associationStorage(AssociationStorageType storage);

}
