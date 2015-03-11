/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.impl;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.ogm.failure.ErrorHandler;

/**
 * A {@link TransactionFactory} which takes transactions created by another factory and decorates them with the
 * {@link ErrorHandlerEnabledTransactionDecorator}.
 *
 * @author Gunnar Morling
 */
public class ErrorHandlerEnabledTransactionDecoratorFactory implements TransactionFactory<ErrorHandlerEnabledTransactionDecorator> {

	private final ErrorHandler errorHandler;
	private final TransactionFactory<?> delegate;
	private final JtaPlatform jtaPlatform;

	public ErrorHandlerEnabledTransactionDecoratorFactory(ErrorHandler errorHandler, TransactionFactory<?> delegate, JtaPlatform jtaPlatform) {
		this.errorHandler = errorHandler;
		this.delegate = delegate;
		this.jtaPlatform = jtaPlatform;
	}

	@Override
	public ErrorHandlerEnabledTransactionDecorator createTransaction(TransactionCoordinator coordinator) {
		TransactionImplementor transaction = delegate.createTransaction( coordinator );
		return new ErrorHandlerEnabledTransactionDecorator( transaction, errorHandler, jtaPlatform );
	}

	@Override
	public boolean canBeDriver() {
		return delegate.canBeDriver();
	}

	@Override
	public boolean compatibleWithJtaSynchronization() {
		return delegate.compatibleWithJtaSynchronization();
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean isJoinableJtaTransaction(TransactionCoordinator transactionCoordinator, ErrorHandlerEnabledTransactionDecorator transaction) {
		return ( (TransactionFactory) delegate ).isJoinableJtaTransaction( transactionCoordinator, transaction.getDelegate() );
	}

	@Override
	public ConnectionReleaseMode getDefaultReleaseMode() {
		return delegate.getDefaultReleaseMode();
	}
}
