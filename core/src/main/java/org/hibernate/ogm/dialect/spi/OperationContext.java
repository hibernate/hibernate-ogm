/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;

/**
 * Provides context information to {@link GridDialect}s about an operation performed on an entity.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public interface OperationContext {

	/**
	 * Provides access to the operations queue of the current flush cycle if the active dialect supports the batched
	 * execution of operations.
	 *
	 * @return the operations queue of the current flush or {@code null} if the active dialect does the batched
	 * execution of operations
	 */
	OperationsQueue getOperationsQueue();

	/**
	 * Provides the information related to the transactional boundaries the query can be executed
	 *
	 * @return a transaction context containing information about the current running transaction, or null
	 */
	TransactionContext getTransactionContext();

	/**
	 * Provides context information related to the given entity's type.
	 * @return Context information related to the given entity's type
	 */
	TupleTypeContext getTupleTypeContext();

}
