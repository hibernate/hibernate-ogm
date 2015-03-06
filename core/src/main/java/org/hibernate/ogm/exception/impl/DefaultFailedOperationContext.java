/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.impl;

import java.util.List;

import org.hibernate.ogm.exception.operation.spi.GridDialectOperation;
import org.hibernate.ogm.exception.spi.ErrorHandler;

/**
 * @author Gunnar Morling
 *
 */
public class DefaultFailedOperationContext implements ErrorHandler.FailedOperationContext {

	private final GridDialectOperation failedOperation;
	private final List<GridDialectOperation> appliedOperations;
	private final Exception exception;

	public DefaultFailedOperationContext(GridDialectOperation failedOperation, List<GridDialectOperation> appliedOperations, Exception exception) {
		this.failedOperation = failedOperation;
		this.appliedOperations = appliedOperations;
		this.exception = exception;
	}

	@Override
	public GridDialectOperation getFailedOperation() {
		return failedOperation;
	}

	@Override
	public List<GridDialectOperation> getAppliedOperations() {
		return appliedOperations;
	}

	@Override
	public Exception getException() {
		return exception;
	}
}
