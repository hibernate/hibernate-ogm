/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.key.spi;

import java.util.Collection;
import java.util.List;

/**
 * The type of an association.
 *
 * @author Gunnar Morling
 */
public enum AssociationType {

	/**
	 * A {@link Collection} or {@link List} without an order column.
	 */
	BAG,

	LIST,

	SET,

	MAP,

	ONE_TO_ONE;
}
