/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ogm.exception.operation.spi.GridDialectOperation;
import org.hibernate.ogm.exception.spi.ErrorHandler;
import org.hibernate.service.Service;

/**
 * Manages the {@link ErrorHandler}, if present.
 *
 * @author Gunnar Morling
 */
public class ErrorHandlerService implements Service {

	private final GridDialectInvocationCollector invocationCollector;
	private final ErrorHandler errorHandler;

	private final ThreadLocal<List<GridDialectOperation>> appliedOperations = new ThreadLocal<List<GridDialectOperation>>() {

		@Override
		protected List<GridDialectOperation> initialValue() {
			return new ArrayList<GridDialectOperation>();
		};
	};

	public ErrorHandlerService(GridDialectInvocationCollector invocationCollector, ErrorHandler errorHandler) {
		this.invocationCollector = invocationCollector;
		this.errorHandler = errorHandler;
	}

	/**
	 * Captures the applied operations of the current flush cycle from the collector before they are cleared.
	 */
	public void onFlush() {
		appliedOperations.get().addAll( invocationCollector.getAppliedOperationsOfFlushCycle() );
	}

	public void onCommit() {
		appliedOperations.remove();
	}

	public void onRollback() {
		try {
			DefaultRollbackContext context = new DefaultRollbackContext( appliedOperations.get() );
			errorHandler.onRollback( context );
		}
		finally {
			appliedOperations.remove();
		}
	}
}
