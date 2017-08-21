/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.util.impl.Contracts;

/**
 * Provides context information to {@link GridDialect}s when accessing {@link Association}s.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class AssociationContextImpl implements AssociationContext {

	private final AssociationTypeContext associationTypeContext;
	private final OperationsQueue operationsQueue;
	private final TuplePointer entityTuplePointer;
	private final TransactionContext transactionContext;

	public AssociationContextImpl(AssociationTypeContext associationTypeContext, TuplePointer entityTuplePointer, TransactionContext transactionContext) {
		this( associationTypeContext, entityTuplePointer, null, transactionContext );
	}

	public AssociationContextImpl(AssociationContextImpl original, OperationsQueue operationsQueue) {
		this( original.associationTypeContext, original.entityTuplePointer, operationsQueue, original.transactionContext );
	}

	private AssociationContextImpl(AssociationTypeContext associationTypeContext,
			TuplePointer entityTuplePointer,
			OperationsQueue operationsQueue,
			TransactionContext transactionContext) {
		Contracts.assertParameterNotNull( associationTypeContext, "associationTypeContext" );

		this.associationTypeContext = associationTypeContext;
		this.entityTuplePointer = entityTuplePointer;
		this.operationsQueue = operationsQueue;
		this.transactionContext = transactionContext;
	}

	@Override
	public AssociationTypeContext getAssociationTypeContext() {
		return associationTypeContext;
	}

	@Override
	public OperationsQueue getOperationsQueue() {
		return operationsQueue;
	}

	@Override
	public TransactionContext getTransactionContext() {
		return transactionContext;
	}

	@Override
	public TuplePointer getEntityTuplePointer() {
		return entityTuplePointer;
	}

	@Override
	public TupleTypeContext getTupleTypeContext() {
		return associationTypeContext.getHostingEntityTupleTypeContext();
	}

	@Override
	public String toString() {
		return "AssociationContextImpl [associationTypeContext=" + associationTypeContext + ", operationsQueue=" + operationsQueue + ", entityTuple="
				+ entityTuplePointer + "]";
	}
}
