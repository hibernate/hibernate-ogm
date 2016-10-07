/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;

/**
 * Represents all information used to load an entity with some specific characteristics like a projection
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class TupleContextImpl implements TupleContext {

	private final TupleTypeContext tupleTypeContext;
	private final OperationsQueue operationsQueue;
	private final TransactionContext transactionContext;

	public TupleContextImpl(TupleContextImpl original, OperationsQueue operationsQueue) {
		this( original.tupleTypeContext, operationsQueue, original.transactionContext );
	}

	public TupleContextImpl(TupleContextImpl original, TransactionContext transactionContext) {
		this( original.tupleTypeContext, original.operationsQueue, transactionContext );
	}

	public TupleContextImpl(TupleTypeContext tupleTypeContext, TransactionContext transactionContext) {
		this( tupleTypeContext, null, transactionContext );
	}

	public TupleContextImpl(TupleTypeContext tupleTypeContext) {
		this( tupleTypeContext, null, null );
	}

	private TupleContextImpl(TupleTypeContext tupleTypeContext, OperationsQueue operationsQueue, TransactionContext transactionContext) {
		this.tupleTypeContext = tupleTypeContext;
		this.operationsQueue = operationsQueue;
		this.transactionContext = transactionContext;
	}

	@Override
	public TransactionContext getTransactionContext() {
		return transactionContext;
	}

	@Override
	public OperationsQueue getOperationsQueue() {
		return operationsQueue;
	}

	@Override
	public TupleTypeContext getTupleTypeContext() {
		return tupleTypeContext;
	}

}
