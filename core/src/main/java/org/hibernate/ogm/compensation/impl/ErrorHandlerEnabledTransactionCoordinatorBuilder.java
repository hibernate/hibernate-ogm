/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation.impl;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.ogm.compensation.ErrorHandler;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * A {@link TransactionCoordinatorBuilder} which takes transactions created by another builder and decorates them with the
 * {@link ErrorHandlerEnabledTransactionCoordinatorDecorator}.
 *
 * @author Gunnar Morling
 */
public class ErrorHandlerEnabledTransactionCoordinatorBuilder implements TransactionCoordinatorBuilder {

	private final TransactionCoordinatorBuilder delegate;
	private final ErrorHandler errorHandler;

	public ErrorHandlerEnabledTransactionCoordinatorBuilder(TransactionCoordinatorBuilder delegate, ErrorHandler errorHandler) {
		this.delegate = delegate;
		this.errorHandler = errorHandler;
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, TransactionCoordinatorOptions options) {
		return new ErrorHandlerEnabledTransactionCoordinatorDecorator(
				delegate.buildTransactionCoordinator( owner, options ),
				errorHandler
		);
	}

	@Override
	public boolean isJta() {
		return delegate.isJta();
	}

	@Override
	public ConnectionReleaseMode getDefaultConnectionReleaseMode() {
		return delegate.getDefaultConnectionReleaseMode();
	}

	@Override
	public ConnectionAcquisitionMode getDefaultConnectionAcquisitionMode() {
		return delegate.getDefaultConnectionAcquisitionMode();
	}
}
