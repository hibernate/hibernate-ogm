/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Supplies {@link Tuple}s.
 *
 * @author Davide D'Alto
 */
public interface TuplesSupplier {

	/**
	 * @param transactionContext the current {@link TransactionContext}, can be {@code null}
	 * @return a closable iterator to scan the tuples
	 */
	ClosableIterator<Tuple> get( TransactionContext transactionContext );
}
