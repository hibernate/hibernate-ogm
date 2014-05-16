/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.options.navigation;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.options.navigation.EntityContext;

/**
 * Allows to configure document store options applying on a per-entity level. These options can be overridden for single
 * properties.
 *
 * @author Gunnar Morling
 */
public interface DocumentStoreEntityContext<E extends DocumentStoreEntityContext<E, P>, P extends DocumentStorePropertyContext<E, P>> extends EntityContext<E, P> {

	/**
	 * Specifies how associations of the configured entity should be persisted.
	 *
	 * @param associationStorage the association storage type to be used when not configured on the property level.
	 * Overrides any settings on the global level.
	 * @return this context, allowing for further fluent API invocations
	 */
	E associationStorage(AssociationStorageType associationStorage);
}
