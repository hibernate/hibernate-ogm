/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.impl;

import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.ogm.exception.impl.ErrorHandlerManager;
import org.hibernate.ogm.exception.spi.ErrorHandler;

/**
 * A {@link TransactionImplementor} wrapper which triggers invocation of the user-configured {@link ErrorHandler} upon
 * failed operations, rollbacks etc.
 *
 * @author Gunnar Morling
 */
public class ErrorHandlerEnabledTransaction extends ForwardingTransactionImplementor {

	private ErrorHandlerManager errorHandlerManager;

	public ErrorHandlerEnabledTransaction(TransactionImplementor delegate, ErrorHandlerManager errorHandlerManager) {
		super( delegate );
		this.errorHandlerManager = errorHandlerManager;
	}

	/**
	 * Begins the transaction, using the given error handler. Intended for OGM-internal testing only at this point.
	 */
	public void begin(ErrorHandler errorHandler) {
		errorHandlerManager = new ErrorHandlerManager( errorHandler );
		super.begin();
	}

	@Override
	public void rollback() {
		errorHandlerManager.onRollback();
		super.rollback();
	}

	public ErrorHandlerManager getErrorHandlerManager() {
		return errorHandlerManager;
	}
}
