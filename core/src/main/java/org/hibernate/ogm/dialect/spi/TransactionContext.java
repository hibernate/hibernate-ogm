/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

/**
 * Contains information about the running transaction.
 *
 * @author Davide D'Alto
 */
public interface TransactionContext {

	/**
	 * A value that can be used to identify the running transaction.
	 *
	 * @return the transaction identifier
	 */
	Object getTransactionId();
}
