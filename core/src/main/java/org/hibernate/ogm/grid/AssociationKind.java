/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.grid;

import javax.persistence.ElementCollection;

/**
 * The kind of an association.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public enum AssociationKind {

	/**
	 * An association to another entity.
	 */
	ASSOCIATION,

	/**
	 * An embedded element collection.
	 *
	 * @see ElementCollection
	 */
	EMBEDDED_COLLECTION
}
