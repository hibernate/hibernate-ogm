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
import org.hibernate.ogm.model.spi.Association;

/**
 * Provides context information to {@link GridDialect}s when accessing {@link Association}s.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class AssociationContextImpl implements AssociationContext {

	private final AssociationTypeContext associationTypeContext;
	private final OperationsQueue operationsQueue;

	public AssociationContextImpl(AssociationTypeContext associationTypeContext) {
		this(associationTypeContext, null );
	}

	public AssociationContextImpl(AssociationContextImpl original, OperationsQueue operationsQueue) {
		this( original.associationTypeContext, operationsQueue );
	}

	private AssociationContextImpl(AssociationTypeContext associationTypeContext, OperationsQueue operationsQueue) {
		this.associationTypeContext = associationTypeContext;
		this.operationsQueue = operationsQueue;
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
	public String toString() {
		return "AssociationContextImpl [associationTypeContext=" + associationTypeContext + ", operationsQueue=" + operationsQueue + "]";
	}
}
