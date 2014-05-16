/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.options.navigation;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.options.navigation.GlobalContext;

/**
 * Allows to configure document store options applying on a global level. These options may be overridden for single
 * entities or properties.
 *
 * @author Gunnar Morling
 */
public interface DocumentStoreGlobalContext<G extends DocumentStoreGlobalContext<G, E>, E extends DocumentStoreEntityContext<E, ?>> extends GlobalContext<G, E> {

	/**
	 * Specifies how associations should be persisted.
	 *
	 * @param associationStorage the association storage type to be used when not configured on the entity or property
	 * level
	 * @return this context, allowing for further fluent API invocations
	 */
	G associationStorage(AssociationStorageType associationStorage);
}
