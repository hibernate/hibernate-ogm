/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.emulated.impl;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinatorOwner;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransactionAccess;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.resource.transaction.spi.TransactionStatus;

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
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options) {
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
		return ConnectionAcquisitionMode.IMMEDIATELY;
	}

	/**
	 * Makes sure a no-op {@link JdbcResourceTransaction} is used.
	 */
	private static class NoopJdbcResourceTransactionCoordinatorOwner extends ForwardingTransactionCoordinatorOwner implements JdbcResourceTransactionAccess {

		public NoopJdbcResourceTransactionCoordinatorOwner(TransactionCoordinatorOwner delegate) {
			super( delegate );
		}

		@Override
		public JdbcResourceTransaction getResourceLocalTransaction() {
			return new NoopJdbcResourceTransaction();
		}
	}

	/**
	 * No-op {@link JdbcResourceTransaction}.
	 */
	private static class NoopJdbcResourceTransaction implements JdbcResourceTransaction {

		private TransactionStatus status;

		@Override
		public void begin() {
			status = TransactionStatus.ACTIVE;
		}

		@Override
		public void commit() {
			status = TransactionStatus.NOT_ACTIVE;
		}

		@Override
		public void rollback() {
			status = TransactionStatus.NOT_ACTIVE;
		}

		@Override
		public TransactionStatus getStatus() {
			return status;
		}
	}

	@Override
	public PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode() {
		return delegate.getDefaultConnectionHandlingMode();
	}
}
