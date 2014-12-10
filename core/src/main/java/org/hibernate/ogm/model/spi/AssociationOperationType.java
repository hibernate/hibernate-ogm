/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.spi;

/**
 * Type of operations on the tuple
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public enum AssociationOperationType {
	/**
	 * The PUT operation is never for a null value.
	 * Use REMOVE instead.
	 */
	PUT,
	REMOVE,
	CLEAR
}
