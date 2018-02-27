/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation.impl;

import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.ogm.compensation.ErrorHandler;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

/**
 * A {@link TransactionCoordinator} invoking the registered {@link ErrorHandler} upon rollbacks.
 *
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public class ErrorHandlerEnabledTransactionCoordinatorDecorator extends ForwardingTransactionCoordinator {

	private final ErrorHandler errorHandler;

	private OperationCollector operationCollector;

	public ErrorHandlerEnabledTransactionCoordinatorDecorator(TransactionCoordinator delegate, ErrorHandler errorHandler) {
		super( delegate );

		this.errorHandler = errorHandler;

		delegate.addObserver( new OperationCollectorObserver() );
	}

	public OperationCollector getOperationCollector() {
		return operationCollector;
	}

	@Override
	public void pulse() {
		// Create the collector upon first usage in a given TX (not done via TransactionObserver#afterBegin() as it is
		// not invoked in the case of JTA
		if ( operationCollector == null ) {
			operationCollector = new OperationCollector( errorHandler );
		}

		super.pulse();
	}

	/**
	 * Invokes the error handler upon TX rollback. Implemented via a observer as this nicely works for local and JTA
	 * transactions.
	 */
	private class OperationCollectorObserver implements TransactionObserver {

		@Override
		public void afterBegin() {
			// Nothing to do
		}

		@Override
		public void beforeCompletion() {
			// Nothing to do
		}

		@Override
		public void afterCompletion(boolean successful, boolean delayed) {
			if ( !successful ) {
				errorHandler.onRollback( operationCollector );
			}

			operationCollector = null;
		}
	}
}
