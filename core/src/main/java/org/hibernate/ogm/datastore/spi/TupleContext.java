/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012-2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.spi;

import org.hibernate.ogm.dialect.batch.OperationsQueue;

/**
 * Represents all information used to load an entity with some specific characteristics like a projection
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 * @author Gunnar Morling
 */
public class TupleContext implements GridDialectOperationContext {

	private final TupleTypeContext tupleTypeContext;
	private final SessionContext sessionContext;
	private final OperationsQueue operationsQueue;

	public TupleContext(TupleTypeContext tupleTypeContext, SessionContext sessionContext) {
		this.tupleTypeContext = tupleTypeContext;
		this.sessionContext = sessionContext;
		this.operationsQueue = null;
	}

	public TupleContext(TupleTypeContext tupleTypeContext, SessionContext sessionContext, OperationsQueue operationsQueue) {
		this.tupleTypeContext = tupleTypeContext;
		this.sessionContext = sessionContext;
		this.operationsQueue = operationsQueue;
	}

	public TupleTypeContext getTupleTypeContext() {
		return tupleTypeContext;
	}

	@Override
	public SessionContext getSessionContext() {
		return sessionContext;
	}

	/**
	 * Provides access to the operations queue of the current flush cycle if the active dialect supports the batched
	 * execution of operations.
	 *
	 * @return the operations queue of the current flush or {@code null} if the active dialect does the batched
	 * execution of operations
	 */
	public OperationsQueue getOperationsQueue() {
		return operationsQueue;
	}
}
