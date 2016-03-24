/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.emulated.impl;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * Builds non-JTA-based {@link TransactionCoordinator}s which use a no-op {@link JdbcResourceTransaction} and
 * connection. That way flushes are triggered as expected - and written to the datastore, but no actual transaction is
 * involved.
 *
 * @author Gunnar Morling
 */
public class EmulatedLocalTransactionCoordinatorBuilder implements TransactionCoordinatorBuilder {

	private final TransactionCoordinatorBuilder delegate;

	public EmulatedLocalTransactionCoordinatorBuilder(TransactionCoordinatorBuilder delegate) {
		this.delegate = delegate;
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, TransactionCoordinatorOptions options) {
		return delegate.buildTransactionCoordinator( new NoopJdbcResourceTransactionCoordinatorOwner( owner ), options );
	}

	@Override
	public boolean isJta() {
		return false;
	}

	@Override
	public ConnectionReleaseMode getDefaultConnectionReleaseMode() {
		return ConnectionReleaseMode.AFTER_TRANSACTION;
	}

	@Override
	public ConnectionAcquisitionMode getDefaultConnectionAcquisitionMode() {
		return ConnectionAcquisitionMode.DEFAULT;
	}
}
