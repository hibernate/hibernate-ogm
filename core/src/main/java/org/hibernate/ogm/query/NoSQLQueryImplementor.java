/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.query;

import org.hibernate.query.spi.NativeQueryImplementor;

/**
 * A native NoSQL query, in a store-specific representation.
 *
 * @author Guillaume Smet
 */
public interface NoSQLQueryImplementor<T> extends NativeQueryImplementor<T>, NoSQLQuery<T> {
}
