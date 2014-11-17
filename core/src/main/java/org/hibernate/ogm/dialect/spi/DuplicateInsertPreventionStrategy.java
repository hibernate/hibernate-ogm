/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.HibernateException;

/**
 * A strategy for preventing the insertion of several entity tuples with the same primary key.
 * <p>
 * No matter which strategy is used, a {@link HibernateException} will be raised by the Hibernate OGM once the insertion
 * of a duplicate key is detected.
 *
 * @author Gunnar Morling
 */
public enum DuplicateInsertPreventionStrategy {

	/**
	 * Prior to inserting a new entity tuple, a read of that entity key is performed. If not configured otherwise by a
	 * specific grid dialect implementation, this strategy is the default behavior as it is portable across all
	 * datastores. It is not optimal for several reasons though:
	 * <ul>
	 * <li>The additional round-trip to the datastore has negative performance implications</li>
	 * <li>As the read and the following insert are not an atomic operation, there is a small time window left within
	 * which duplicate inserts may go unnoticed</li>
	 * </ul>
	 */
	LOOK_UP,

	/**
	 * The datastore itself raises a meaningful exception whenever it detects the insertion of a duplicate key. The
	 * dialect implementation is expected to catch the datastore-specific exception and wrap it within a
	 * {@link TupleAlreadyExistsException}. Recommended strategy for all stores where it is feasible.
	 */
	NATIVE,
}
