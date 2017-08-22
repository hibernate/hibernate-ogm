/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.jta.impl;

import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinator;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinatorBuilder;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionDriver;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinator.TransactionDriver;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * Decorator for JTA-based {@link TransactionCoordinatorBuilder}s.
 * <p>
 * Used with the {@code RESOURCE_LOCAL} transaction strategy in case we need to actually use JTA for the given backend.
 * In that case the user cannot roll back the transaction themselves upon commit failures (as ORM will only allow that
 * for the transaction states {@code ACTIVE} and {@code FAILED_COMMIT}, whereas it will be {@code MARKED_ROLLBACK} in
 * this case. This implementation does the rollback in this situation.
 *
 * @author Gunnar Morling
 */
public class RollbackOnCommitFailureJtaTransactionCoordinatorBuilder extends ForwardingTransactionCoordinatorBuilder {

	public RollbackOnCommitFailureJtaTransactionCoordinatorBuilder(TransactionCoordinatorBuilder delegate) {
		super( delegate );
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options) {
		return new RollbackOnCommitTransactionCoordinator( super.buildTransactionCoordinator( owner, options ) );
	}

	private static class RollbackOnCommitTransactionCoordinator extends ForwardingTransactionCoordinator {

		public RollbackOnCommitTransactionCoordinator(TransactionCoordinator delegate) {
			super( delegate );
		}

		@Override
		public TransactionDriver getTransactionDriverControl() {
			return new RollbackOnCommitFailureTransactionDriver( super.getTransactionDriverControl() );
		}
	}

	private static class RollbackOnCommitFailureTransactionDriver extends ForwardingTransactionDriver {

		public RollbackOnCommitFailureTransactionDriver(TransactionDriver delegate) {
			super( delegate );
		}

		@Override
		public void commit() {
			try {
				super.commit();
			}
			catch (Exception e) {
				try {
					rollback();
				}
				catch (Exception rollbackException) {
				}

				throw e;
			}
		}
	}
}
