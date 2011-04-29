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
package org.hibernate.ogm.transaction.infinispan.impl;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.slf4j.Logger;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.util.JTAHelper;

/**
 * Transaction implementation using JTA transactions exclusively from the TransactionManager
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class JTATransactionManagerTransaction implements Transaction {

	private final Logger log = LoggerFactory.make();
	private final JDBCContext jdbcContext;
	private final TransactionFactory.Context transactionContext;
	private TransactionManager transactionManager;
	private boolean begun;
	private boolean commitFailed;
	private boolean newTransaction;
	private boolean callback;
	private boolean commitSucceeded;

	public JTATransactionManagerTransaction(
			JDBCContext jdbcContext,
			TransactionFactory.Context transactionContext) {
		this.jdbcContext = jdbcContext;
		this.transactionContext = transactionContext;
		this.transactionManager = transactionContext.getFactory().getTransactionManager();
	}

	public void begin() throws HibernateException {
		if ( begun ) {
			return;
		}
		if ( commitFailed ) {
			throw new TransactionException( "cannot re-start transaction after failed commit" );
		}

		log.trace( "begin" );

		try {
			newTransaction = transactionManager.getStatus() == Status.STATUS_NO_TRANSACTION;
			if ( newTransaction ) {
				transactionManager.begin();
				log.trace( "Began a new JTA transaction" );
			}
		}
		catch ( Exception e ) {
			log.error( "JTA transaction begin failed", e );
			throw new TransactionException( "JTA transaction begin failed", e );
		}

		/*if (newTransaction) {
			// don't need a synchronization since we are committing
			// or rolling back the transaction ourselves - assuming
			// that we do no work in beforeTransactionCompletion()
			synchronization = false;
		}*/

		boolean synchronization = jdbcContext.registerSynchronizationIfPossible();

		if ( !newTransaction && !synchronization ) {
			log.warn( "You should set hibernate.transaction.manager_lookup_class if cache is enabled" );
		}

		if ( !synchronization ) {
			//if we could not register a synchronization,
			//do the before/after completion callbacks
			//ourself (but we need to let jdbcContext
			//know that this is what we are going to
			//do, so it doesn't keep trying to register
			//synchronizations)
			callback = jdbcContext.registerCallbackIfNecessary();
		}

		begun = true;
		commitSucceeded = false;

		jdbcContext.afterTransactionBegin( this );
	}

	public void commit() throws HibernateException {
		if ( !begun ) {
			throw new TransactionException( "Transaction not successfully started" );
		}

		log.trace( "commit" );

		boolean flush = !transactionContext.isFlushModeNever()
				&& ( callback || !transactionContext.isFlushBeforeCompletionEnabled() );

		if ( flush ) {
			transactionContext.managedFlush(); //if an exception occurs during flush, user must call rollback()
		}

		if ( callback && newTransaction ) {
			jdbcContext.beforeTransactionCompletion( this );
		}

		closeIfRequired();

		if ( newTransaction ) {
			try {
				transactionManager.commit();
				commitSucceeded = true;
				log.trace( "Committed JTA UserTransaction" );
			}
			catch ( Exception e ) {
				commitFailed = true; // so the transaction is already rolled back, by JTA spec
				log.error( "JTA commit failed", e );
				throw new TransactionException( "JTA commit failed: ", e );
			}
			finally {
				afterCommitRollback();
			}
		}
		else {
			// this one only really needed for badly-behaved applications!
			// (if the TransactionManager has a Sychronization registered,
			// its a noop)
			// (actually we do need it for downgrading locks)
			afterCommitRollback();
		}
	}

	private void closeIfRequired() throws HibernateException {
		boolean close = callback &&
				transactionContext.shouldAutoClose() &&
				!transactionContext.isClosed();
		if ( close ) {
			transactionContext.managedClose();
		}
	}

	private static final int NULL = Integer.MIN_VALUE;

	private void afterCommitRollback() throws TransactionException {

		begun = false;
		// this method is a noop if there is a Synchronization!
		if ( callback ) {
			if ( !newTransaction ) {
				log.warn( "You should set hibernate.transaction.manager_lookup_class if cache is enabled" );
			}
			int status = NULL;
			try {
				status = transactionManager.getStatus();
			}
			catch ( Exception e ) {
				log.error( "Could not determine transaction status after commit", e );
				throw new TransactionException( "Could not determine transaction status after commit", e );
			}
			finally {
				jdbcContext.afterTransactionCompletion( status == Status.STATUS_COMMITTED, this );
			}
		}
	}

	public void rollback() throws HibernateException {
		if ( !begun && !commitFailed ) {
			throw new TransactionException( "Transaction not successfully started" );
		}

		log.trace( "rollback" );

		try {
			closeIfRequired();
		}
		catch ( Exception e ) {
			// swallow it, and continue to roll back JTA transaction
			log.error( "could not close session during rollback", e );
		}

		try {
			if ( newTransaction ) {
				if ( !commitFailed ) {
					transactionManager.rollback();
					log.debug( "Rolled back JTA UserTransaction" );
				}
			}
			else {
				transactionManager.setRollbackOnly();
				log.trace( "set JTA UserTransaction to rollback only" );
			}
		}
		catch ( Exception e ) {
			log.error( "JTA rollback failed", e );
			throw new TransactionException( "JTA rollback failed", e );
		}
		finally {
			afterCommitRollback();
		}
	}

	public boolean wasRolledBack() throws HibernateException {
		final int status;
		try {
			status = transactionManager.getStatus();
		}
		catch ( SystemException se ) {
			log.error( "Could not determine transaction status", se );
			throw new TransactionException( "Could not determine transaction status", se );
		}
		if ( status == Status.STATUS_UNKNOWN ) {
			throw new TransactionException( "Could not determine transaction status" );
		}
		else {
			return JTAHelper.isRollback( status );
		}
	}

	public boolean wasCommitted() throws HibernateException {
		final int status;
		try {
			status = transactionManager.getStatus();
		}
		catch ( SystemException se ) {
			log.error( "Could not determine transaction status", se );
			throw new TransactionException( "Could not determine transaction status: ", se );
		}
		if ( status == Status.STATUS_UNKNOWN ) {
			throw new TransactionException( "Could not determine transaction status" );
		}
		else {
			return status == Status.STATUS_COMMITTED;
		}
	}

	public boolean isActive() throws HibernateException {
		if ( !begun || commitFailed || commitSucceeded ) {
			return false;
		}

		final int status;
		try {
			status = transactionManager.getStatus();
		}
		catch ( SystemException se ) {
			log.error( "Could not determine transaction status", se );
			throw new TransactionException( "Could not determine transaction status: ", se );
		}
		if ( status == Status.STATUS_UNKNOWN ) {
			throw new TransactionException( "Could not determine transaction status" );
		}
		else {
			return status == Status.STATUS_ACTIVE;
		}
	}

	public void registerSynchronization(Synchronization synchronization) throws HibernateException {
		if ( transactionManager == null ) {
			throw new IllegalStateException( "JTA TransactionManager not available" );
		}
		else {
			try {
				transactionManager.getTransaction().registerSynchronization( synchronization );
			}
			catch ( Exception e ) {
				throw new TransactionException( "could not register synchronization", e );
			}
		}
	}

	public void setTimeout(int seconds) {
		try {
			transactionManager.setTransactionTimeout( seconds );
		}
		catch ( SystemException se ) {
			throw new TransactionException( "could not set transaction timeout", se );
		}
	}
}
