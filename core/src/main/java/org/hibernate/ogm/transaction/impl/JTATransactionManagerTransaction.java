/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Sanne Grinovero  <sanne@hibernate.org>
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
