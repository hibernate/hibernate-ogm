/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.batch.OperationsQueue;
import org.hibernate.ogm.options.spi.OptionsContext;

/**
 * Provides context information to {@link GridDialect}s when accessing {@link Association}s.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Gunnar Morling
 */
public class AssociationContext implements GridDialectOperationContext {

	private final OptionsContext optionsContext;
	private final OperationsQueue operationsQueue;

	public AssociationContext(OptionsContext optionsContext) {
		this( optionsContext, null );
	}

	public AssociationContext(OptionsContext optionsContext, OperationsQueue operationsQueue) {
		this.optionsContext = optionsContext;
		this.operationsQueue = operationsQueue;
	}

	public OperationsQueue getOperationsQueue() {
		return operationsQueue;
	}

	@Override
	public OptionsContext getOptionsContext() {
		return optionsContext;
	}

	@Override
	public String toString() {
		return "AssociationContext [optionsContext=" + optionsContext + "]";
	}
}
