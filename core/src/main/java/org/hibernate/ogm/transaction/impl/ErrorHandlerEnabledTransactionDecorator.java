/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.impl;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.ogm.failure.ErrorHandler;
import org.hibernate.ogm.failure.impl.OperationCollector;

/**
 * A {@link TransactionImplementor} wrapper which triggers invocation of the user-configured {@link ErrorHandler} upon
 * failed operations, rollbacks etc.
 * <p>
 * Depending on the type of the underlying transacton (JTA or not),
 * {@link ErrorHandler#onRollback(org.hibernate.ogm.failure.ErrorHandler.RollbackContext)} will either be invoked
 * via a {@link Synchronization} or through the {@link #rollback()} method.
 *
 * @author Gunnar Morling
 */
public class ErrorHandlerEnabledTransactionDecorator extends ForwardingTransactionImplementor {

	private final JtaPlatform jtaPlatform;

	private ErrorHandler errorHandler;
	private OperationCollector operationCollector;
	private boolean callErrorHandlerOnRollback;

	public ErrorHandlerEnabledTransactionDecorator(TransactionImplementor delegate, ErrorHandler errorHandler, JtaPlatform jtaPlatform) {
		super( delegate );

		this.jtaPlatform = jtaPlatform;

		this.errorHandler = errorHandler;
		this.operationCollector = new OperationCollector();
		this.callErrorHandlerOnRollback = true;
	}

	/**
	 * Begins the transaction, using the given error handler. Intended for OGM-internal testing only at this point.
	 */
	public void begin(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
		this.operationCollector = new OperationCollector();

		super.begin();
	}

	@Override
	public void rollback() {
		if ( callErrorHandlerOnRollback ) {
			errorHandler.onRollback( operationCollector );
		}
		super.rollback();
	}

	public OperationCollector getOperationCollector() {
		return operationCollector;
	}

	@Override
	public void join() {
		boolean synchronizationAlreadyRegistered = !callErrorHandlerOnRollback;
		boolean jtaTransactionActive = jtaPlatform != null && JtaStatusHelper.isActive( jtaPlatform.retrieveTransactionManager() );

		if ( jtaTransactionActive && !synchronizationAlreadyRegistered ) {
			jtaPlatform.registerSynchronization( new ErrorHandlerNotificationSynchronization() );
			callErrorHandlerOnRollback = false;
		}

		super.join();
	}

	/**
	 * Notifies the {@link ErrorHandler} about the applied operations upon transaction rollback.
	 */
	private class ErrorHandlerNotificationSynchronization implements Synchronization {

		@Override
		public void beforeCompletion() {
		}

		@Override
		public void afterCompletion(int status) {
			if ( Status.STATUS_ROLLEDBACK == status ) {
				errorHandler.onRollback( operationCollector );
			}
		}
	}
}
