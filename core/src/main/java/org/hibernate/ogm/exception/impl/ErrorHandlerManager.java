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
import org.hibernate.ogm.exception.spi.ErrorHandlingStrategy;

/**
 * Manages the {@link ErrorHandler}, if present. The lifecyle of instances is tied to transactions.
 *
 * @author Gunnar Morling
 */
public class ErrorHandlerManager {

	private final ErrorHandler errorHandler;
	private final List<GridDialectOperation> appliedOperations;

	public ErrorHandlerManager(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
		this.appliedOperations = new ArrayList<>();
	}

	/**
	 * Captures the applied operations of the one flush cycle from the collector before they are cleared.
	 */
	public void afterFlush(List<GridDialectOperation> appliedOperations) {
		this.appliedOperations.addAll( appliedOperations );
	}

	public void onRollback() {
		DefaultRollbackContext context = new DefaultRollbackContext( appliedOperations );
		errorHandler.onRollback( context );
	}

	public ErrorHandlingStrategy onFailedOperation(GridDialectOperation operation, Exception exception) {
		DefaultFailedOperationContext context = new DefaultFailedOperationContext( operation, appliedOperations, exception );
		return errorHandler.onFailedOperation( context );
	}
}
