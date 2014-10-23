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
import org.hibernate.ogm.model.spi.Tuple;
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
	private final Tuple entityTuple;

	public AssociationContextImpl(AssociationTypeContext associationTypeContext, Tuple entityTuple) {
		this( associationTypeContext, entityTuple, null );
	}

	public AssociationContextImpl(AssociationContextImpl original, OperationsQueue operationsQueue) {
		this( original.associationTypeContext, original.entityTuple, operationsQueue );
	}

	private AssociationContextImpl(AssociationTypeContext associationTypeContext, Tuple entityTuple, OperationsQueue operationsQueue) {
		Contracts.assertParameterNotNull( associationTypeContext, "associationTypeContext" );

		this.associationTypeContext = associationTypeContext;
		this.entityTuple = entityTuple;
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
	public Tuple getEntityTuple() {
		return entityTuple;
	}

	@Override
	public String toString() {
		return "AssociationContextImpl [associationTypeContext=" + associationTypeContext + ", operationsQueue=" + operationsQueue + ", entityTuple="
				+ entityTuple + "]";
	}
}
