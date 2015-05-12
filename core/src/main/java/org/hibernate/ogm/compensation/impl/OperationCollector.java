/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.ogm.compensation.ErrorHandler;
import org.hibernate.ogm.compensation.ErrorHandlingStrategy;
import org.hibernate.ogm.compensation.operation.GridDialectOperation;

/**
 * Collects the grid dialect operations applied in the course of one transaction.
 * <p>
 * Implements the error handler context interfaces directly as far as possible in order to avoid allocation of
 * intermediary objects.
 *
 * @author Gunnar Morling
 */
public class OperationCollector implements ErrorHandler.RollbackContext {

	private final ErrorHandler errorHandler;
	private final List<GridDialectOperation> appliedOperations;

	public OperationCollector(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
		this.appliedOperations = new ArrayList<>();
	}

	public void addAppliedOperation(GridDialectOperation operation) {
		appliedOperations.add( operation );
	}

	public ErrorHandlingStrategy onFailedOperation(GridDialectOperation operation, Exception exception) {
		DefaultFailedOperationContext context = new DefaultFailedOperationContext( operation, exception );
		return errorHandler.onFailedGridDialectOperation( context );
	}

	@Override
	public List<GridDialectOperation> getAppliedGridDialectOperations() {
		return Collections.unmodifiableList( appliedOperations );
	}

	private class DefaultFailedOperationContext implements ErrorHandler.FailedGridDialectOperationContext {

		private final GridDialectOperation failedOperation;
		private final Exception exception;

		DefaultFailedOperationContext(GridDialectOperation failedOperation, Exception exception) {
			this.failedOperation = failedOperation;
			this.exception = exception;
		}

		@Override
		public GridDialectOperation getFailedOperation() {
			return failedOperation;
		}

		@Override
		public List<GridDialectOperation> getAppliedGridDialectOperations() {
			return Collections.unmodifiableList( appliedOperations );
		}

		@Override
		public Exception getException() {
			return exception;
		}
	}
}
