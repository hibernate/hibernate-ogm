/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.impl;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.internal.jdbc.JdbcIsolationDelegate;
import org.hibernate.engine.transaction.spi.AbstractTransactionImpl;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.JoinStatus;
import org.hibernate.engine.transaction.spi.LocalStatus;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * A emulated transaction whose only purpose it is to make sure that on commit the appropriate flush events gets triggered.
 *
 * @author Hardy Ferentschik
 */
public class EmulatedLocalTransaction extends AbstractTransactionImpl {
	private static final Log log = LoggerFactory.make();

	private boolean isDriver;

	protected EmulatedLocalTransaction(TransactionCoordinator transactionCoordinator) {
		super( transactionCoordinator );
	}

	@Override
	protected void doBegin() {
		isDriver = transactionCoordinator().takeOwnership();
	}

	@Override
	protected void afterTransactionBegin() {
		transactionCoordinator().sendAfterTransactionBeginNotifications( this );
		if ( isDriver ) {
			transactionCoordinator().getTransactionContext().afterTransactionBegin( this );
		}
	}

	@Override
	protected void beforeTransactionCommit() {
		transactionCoordinator().sendBeforeTransactionCompletionNotifications( this );

		// basically, if we are the driver of the transaction perform a managed flush prior to
		// physically committing the transaction
		if ( isDriver && !transactionCoordinator().getTransactionContext().isFlushModeNever() ) {
			// if an exception occurs during flush, user must call rollback()
			transactionCoordinator().getTransactionContext().managedFlush();
		}

		if ( isDriver ) {
			transactionCoordinator().getTransactionContext().beforeTransactionCompletion( this );
		}
	}

	@Override
	protected void doCommit() throws TransactionException {
	}

	@Override
	protected void afterTransactionCompletion(int status) {
		transactionCoordinator().afterTransaction( this, status );
	}

	@Override
	protected void afterAfterCompletion() {
		if ( isDriver
				&& transactionCoordinator().getTransactionContext().shouldAutoClose()
				&& !transactionCoordinator().getTransactionContext().isClosed() ) {
			try {
				transactionCoordinator().getTransactionContext().managedClose();
			}
			catch ( HibernateException e ) {
				log.unableToCloseSessionButSwallowingError( e );
			}
		}
	}

	@Override
	protected void beforeTransactionRollBack() {
		// nothing to do here
	}

	@Override
	protected void doRollback() throws TransactionException {
	}

	@Override
	public boolean isInitiator() {
		return true;
	}

	@Override
	public IsolationDelegate createIsolationDelegate() {
		return new JdbcIsolationDelegate( transactionCoordinator() );
	}

	@Override
	public JoinStatus getJoinStatus() {
		return isActive() ? JoinStatus.JOINED : JoinStatus.NOT_JOINED;
	}

	@Override
	public void markRollbackOnly() {
		// nothing to do here
	}

	@Override
	public void join() {
		// nothing to do
	}

	@Override
	public void resetJoinStatus() {
		// nothing to do
	}

	@Override
	public boolean isActive() throws HibernateException {
		return getLocalStatus() == LocalStatus.ACTIVE;
	}
}
