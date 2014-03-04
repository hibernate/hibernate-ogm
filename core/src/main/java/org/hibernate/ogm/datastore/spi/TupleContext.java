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

import java.util.List;

import org.hibernate.ogm.dialect.batch.OperationsQueue;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * Represents all information used to load an entity with some specific characteristics like a projection
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 * @author Gunnar Morling
 */
public class TupleContext implements GridDialectOperationContext {

	private final List<String> selectableColumns;
	private final OptionsContext optionsContext;
	private final OperationsQueue operationsQueue;

	public TupleContext(List<String> selectableColumns, OptionsContext optionsContext) {
		this.selectableColumns = selectableColumns;
		this.optionsContext = optionsContext;
		this.operationsQueue = null;
	}

	public TupleContext(List<String> selectableColumns, OptionsContext optionsContext, OperationsQueue operationsQueue) {
		this.selectableColumns = selectableColumns;
		this.optionsContext = optionsContext;
		this.operationsQueue = operationsQueue;
	}

	/**
	 * Returns the mapped columns of the given entity. May be used by a dialect to only load those columns instead of
	 * the complete document/record.
	 */
	public List<String> getSelectableColumns() {
		return selectableColumns;
	}

	@Override
	public OptionsContext getOptionsContext() {
		return optionsContext;
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

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder( "Tuple Context {" );

		builder.append( StringHelper.join( selectableColumns, ", " ) );
		builder.append( "}" );

		return builder.toString();
	}
}
