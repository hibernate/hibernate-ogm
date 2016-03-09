/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import javax.cache.configuration.Factory;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

public class IgniteTransactionManagerFactory implements Factory<TransactionManager> {

	private static final long serialVersionUID = -4649196379875889970L;

	private JtaPlatform platform;

	public IgniteTransactionManagerFactory(JtaPlatform platform) {
		this.platform = platform;
	}

	@Override
	public TransactionManager create() {
		if ( platform != null ) {
			TransactionManager transactionManager = platform.retrieveTransactionManager();
			return transactionManager != null ? new TransactionManagerDelegate(transactionManager) : null;
		}
		else {
			return null;
		}
	}

	/**
	 * because enlistResource(...) throws UnsupportedOperationException
	 * in org.hibernate.service.jta.platform.internal.WebSphereExtendedJtaPlatform.TransactionManagerAdapter.TransactionAdapter
	 * @author Victor Kadachigov
	 */
	private class TransactionManagerDelegate implements TransactionManager {
		private TransactionManager transactionManager;

		public TransactionManagerDelegate(TransactionManager transactionManager) {
			this.transactionManager = transactionManager;
		}

		@Override
		public void begin() throws NotSupportedException, SystemException {
			transactionManager.begin();
		}
		@Override
		public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
			transactionManager.commit();
		}
		@Override
		public int getStatus() throws SystemException {
			return transactionManager.getStatus();
		}
		@Override
		public Transaction getTransaction() throws SystemException {
			Transaction transaction = transactionManager.getTransaction();
			return transaction != null ? new TransactionDelegate(transactionManager.getTransaction()) : null;
		}
		@Override
		public void resume(Transaction tobj) throws InvalidTransactionException, IllegalStateException, SystemException {
			transactionManager.resume( tobj );
		}
		@Override
		public void rollback() throws IllegalStateException, SecurityException, SystemException {
			transactionManager.rollback();
		}
		@Override
		public void setRollbackOnly() throws IllegalStateException, SystemException {
			transactionManager.setRollbackOnly();
		}
		@Override
		public void setTransactionTimeout(int seconds) throws SystemException {
			transactionManager.setTransactionTimeout( seconds );
		}
		@Override
		public Transaction suspend() throws SystemException {
			return transactionManager.suspend();
		}
	}

	private class TransactionDelegate implements Transaction {
		private Transaction transaction;

		public TransactionDelegate(Transaction transaction) {
			this.transaction = transaction;
		}

		@Override
		public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
		{
			transaction.commit();
		}
		@Override
		public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException
		{
			return transaction.delistResource( xaRes, flag );
		}
		@Override
		public boolean enlistResource(XAResource xaRes) throws RollbackException, IllegalStateException, SystemException {
			try {
				return transaction.enlistResource( xaRes );
			}
			catch (UnsupportedOperationException ex) {
				return true;
			}
		}
		@Override
		public int getStatus() throws SystemException {
			return transaction.getStatus();
		}
		@Override
		public void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException, SystemException {
			transaction.registerSynchronization( sync );
		}
		@Override
		public void rollback() throws IllegalStateException, SystemException {
			transaction.rollback();
		}
		@Override
		public void setRollbackOnly() throws IllegalStateException, SystemException {
			transaction.setRollbackOnly();
		}
	}

}
