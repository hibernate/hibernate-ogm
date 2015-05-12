/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.compensation.operation.GridDialectOperation;
import org.hibernate.ogm.util.Experimental;

/**
 * Implementations receive applied and failed grid dialect operations upon exceptions when persisting data in the NoSQL
 * backend.
 * <p>
 * Intended for those stores without transactional semantics, where it allows to determine the changes applied prior to
 * a failure during application of the changes collected for a a unit of work.
 * <p>
 * Instead of implementing this interface directly, implementations should be derived from {@link BaseErrorHandler}, as
 * new methods will be added to the contract in the future. Implementations must be thread-safe.
 * <p>
 * Error handlers are registered via the {@link OgmProperties#ERROR_HANDLER} property.
 *
 * @author Gunnar Morling
 * @see OgmProperties#ERROR_HANDLER
 */
@Experimental
public interface ErrorHandler {

	/**
	 * Callback method invoked if an error occurred during the execution of a grid dialect method. The error handler can
	 * decide whether to abort the current "transaction" or abort it.
	 */
	ErrorHandlingStrategy onFailedGridDialectOperation(FailedGridDialectOperationContext context);

	/**
	 * Callback method invoked if the current "transaction" is rolled back. Provides access to all grid dialect
	 * operations successfully applied before the error causing the rollback occurred.
	 */
	void onRollback(RollbackContext context);

	/**
	 * Provides contextual information when notifying an {@link ErrorHandler} about a failed grid dialect operation.
	 */
	interface FailedGridDialectOperationContext {

		/**
		 * The operation which failed.
		 */
		GridDialectOperation getFailedOperation();

		/**
		 * The operations successfully applied to the datastore before the failure occurred.
		 */
		Iterable<GridDialectOperation> getAppliedGridDialectOperations();

		/**
		 * Exception describing the failure.
		 */
		Exception getException();
	}

	/**
	 * Provides contextual information when notifying an {@link ErrorHandler} about a transaction rollback.
	 */
	interface RollbackContext {

		/**
		 * The operations successfully applied to the datastore before the rollback attempt was made.
		 *
		 * @return An unmodifiable list of successfully applied operations.
		 */
		Iterable<GridDialectOperation> getAppliedGridDialectOperations();
	}
}
