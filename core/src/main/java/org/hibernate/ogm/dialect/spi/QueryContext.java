/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

/**
 * Provides information to {@link GridDialect}s related to a query.
 *
 * @author Davide D'Alto
 */
public interface QueryContext {

	/**
	 * Provides the information related to the transactional boundaries the query can be executed
	 *
	 * @return a transaction context containing information about the current running transaction, or null
	 */
	TransactionContext getTransactionContext();
}
