/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.impl;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.engine.transaction.internal.jta.JtaIsolationDelegate;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.AbstractTransactionImpl;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.JoinStatus;
import org.hibernate.engine.transaction.spi.LocalStatus;
import org.hibernate.engine.transaction.spi.TransactionContext;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Transaction implementation using JTA transactions exclusively from the TransactionManager
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero  &lt;sanne@hibernate.org&gt;
 */
public class JTATransactionManagerTransaction extends AbstractTransactionImpl implements Transaction {

	private static final Log log = LoggerFactory.make();

	private boolean newTransaction;
	private final TransactionManager transactionManager;
	private boolean isDriver;
	private boolean isInitiator;

	public JTATransactionManagerTransaction(TransactionCoordinator coordinator) {
		super( coordinator );
		final JtaPlatform jtaPlatform = coordinator
					.getTransactionContext()
					.getTransactionEnvironment()
					.getJtaPlatform();
		this.transactionManager = jtaPlatform.retrieveTransactionManager();
	}


	@Override
	protected void doBegin() {
		log.trace( "begin" );

		try {
			newTransaction = transactionManager.getStatus() == Status.STATUS_NO_TRANSACTION;
			if ( newTransaction ) {
				transactionManager.begin();
				isInitiator = true;
				log.trace( "Began a new JTA transaction" );
			}
		}
		catch ( Exception e ) {
			throw log.jtaTransactionBeginFailed( e );
		}
	}

	@Override
	protected void afterTransactionBegin() {
		final TransactionCoordinator coordinator = transactionCoordinator();
		coordinator.pulse();

		if ( !coordinator.isSynchronizationRegistered() ) {
			isDriver = coordinator.takeOwnership();
		}

		applyTimeout();
		coordinator.sendAfterTransactionBeginNotifications( this );
		coordinator.getTransactionContext().afterTransactionBegin( this );
	}

	@Override
	protected void beforeTransactionCommit() {
		final TransactionCoordinator coordinator = transactionCoordinator();
		coordinator.sendBeforeTransactionCompletionNotifications( this );
		final TransactionContext transactionContext = coordinator.getTransactionContext();

		final boolean flush = ! transactionContext.isFlushModeNever() &&
				( isDriver || ! transactionContext.isFlushBeforeCompletionEnabled() );

		if ( flush ) {
			// if an exception occurs during flush, user must call rollback()
			transactionContext.managedFlush();
		}

		if ( isDriver && isInitiator ) {
			transactionContext.beforeTransactionCompletion( this );
		}

		closeIfRequired();
	}

	private void closeIfRequired() throws HibernateException {
		final TransactionContext transactionContext = transactionCoordinator().getTransactionContext();
		final boolean close = isDriver &&
				transactionContext.shouldAutoClose() &&
				! transactionContext.isClosed();
		if ( close ) {
			transactionContext.managedClose();
		}
	}

	@Override
	protected void doCommit() {
		try {
			if ( isInitiator ) {
				transactionManager.commit();
				log.debug( "Committed JTA UserTransaction" );
			}
		}
		catch ( Exception e ) {
			throw log.jtaCommitFailed( e );
		}
		finally {
			isInitiator = false;
		}
	}

	@Override
	protected void afterTransactionCompletion(int status) {
		if ( isDriver ) {
			transactionCoordinator().afterTransaction( this, status );
		}
	}

	@Override
	protected void afterAfterCompletion() {
		// nothing to do
	}

	@Override
	protected void beforeTransactionRollBack() {
		// nothing to do
	}

	@Override
	protected void doRollback() {
		try {
			if ( isInitiator ) {
				// failed commits automatically rollback the transaction per JTA spec
				if ( getLocalStatus() != LocalStatus.FAILED_COMMIT ) {
					transactionManager.rollback();
					log.debug( "Rolled back JTA UserTransaction" );
				}
			}
			else {
				markRollbackOnly();
			}
		}
		catch ( Exception e ) {
			throw log.jtaRollbackFailed( e );
		}
	}

	@Override
	public void markRollbackOnly() {
		log.trace( "Marking transaction for rollback only" );
		try {
			transactionManager.setRollbackOnly();
			log.debug( "set JTA UserTransaction to rollback only" );
		}
		catch ( SystemException e ) {
			throw log.unableToMarkTransactionForRollback( e );
		}
	}

	@Override
	public IsolationDelegate createIsolationDelegate() {
		return new JtaIsolationDelegate( transactionCoordinator() );
	}

	@Override
	public boolean isInitiator() {
		return isInitiator;
	}

	@Override
	public boolean isActive() throws HibernateException {
		if ( getLocalStatus() != LocalStatus.ACTIVE ) {
			return false;
		}

		final int status;
		try {
			status = transactionManager.getStatus();
		}
		catch ( SystemException se ) {
			throw log.jtaCouldNotDetermineStatus( se );
		}
		return JtaStatusHelper.isActive( status );
	}

	@Override
	public void setTimeout(int seconds) {
		super.setTimeout( seconds );
		applyTimeout();
	}

	private void applyTimeout() {
		try {
			transactionManager.setTransactionTimeout( getTimeout() );
		}
		catch ( SystemException se ) {
			throw log.unableToSetTimeout( se, getTimeout() );
		}
	}

	@Override
	public void join() {
	}

	@Override
	public void resetJoinStatus() {
	}

	@Override
	public JoinStatus getJoinStatus() {
		return JtaStatusHelper.isActive( transactionManager ) ? JoinStatus.JOINED : JoinStatus.NOT_JOINED;
	}

}
