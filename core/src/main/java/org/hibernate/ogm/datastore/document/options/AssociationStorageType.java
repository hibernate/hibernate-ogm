/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.options;

/**
 * A strategy for storing association information in a document datastore.
 *
 * @author Gunnar Morling
 */
public enum AssociationStorageType {

	/**
	 * Stores association information in a dedicated document per association.
	 */
	ASSOCIATION_DOCUMENT,

	/**
	 * Store association information within the entity.
	 */
	IN_ENTITY
}
