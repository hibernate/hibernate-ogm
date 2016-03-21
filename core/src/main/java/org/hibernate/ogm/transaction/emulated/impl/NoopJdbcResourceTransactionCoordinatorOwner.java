/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.emulated.impl;

import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinatorOwner;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransactionAccess;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * Makes sure a no-op {@link JdbcResourceTransaction} is used.
 */
public class NoopJdbcResourceTransactionCoordinatorOwner extends ForwardingTransactionCoordinatorOwner implements JdbcResourceTransactionAccess {

	public NoopJdbcResourceTransactionCoordinatorOwner(TransactionCoordinatorOwner delegate) {
		super( delegate );
	}

	@Override
	public JdbcResourceTransaction getResourceLocalTransaction() {
		return new NoopJdbcResourceTransaction();
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
}
