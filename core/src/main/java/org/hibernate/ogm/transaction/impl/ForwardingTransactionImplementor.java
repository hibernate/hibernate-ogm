/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.impl;

import javax.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.JoinStatus;
import org.hibernate.engine.transaction.spi.LocalStatus;
import org.hibernate.engine.transaction.spi.TransactionImplementor;

/**
 * A {@link TransactionImplementor} delegating all methods to another {@code TransactionImplementor} instance. Useful as
 * a base class for implementations which need to customize/amend the behavior of the underlying implementation.
 *
 * @author Gunnar Morling
 */
public class ForwardingTransactionImplementor implements TransactionImplementor {

	private final TransactionImplementor delegate;

	public ForwardingTransactionImplementor(TransactionImplementor delegate) {
		this.delegate = delegate;
	}

	@Override
	public IsolationDelegate createIsolationDelegate() {
		return delegate.createIsolationDelegate();
	}

	@Override
	public JoinStatus getJoinStatus() {
		return delegate.getJoinStatus();
	}

	@Override
	public void markForJoin() {
		delegate.markForJoin();
	}

	@Override
	public void join() {
		delegate.join();
	}

	@Override
	public void resetJoinStatus() {
		delegate.resetJoinStatus();
	}

	@Override
	public void markRollbackOnly() {
		delegate.markRollbackOnly();
	}

	@Override
	public void invalidate() {
		delegate.invalidate();
	}

	@Override
	public boolean isInitiator() {
		return delegate.isInitiator();
	}

	@Override
	public void begin() {
		delegate.begin();
	}

	@Override
	public void commit() {
		delegate.commit();
	}

	@Override
	public void rollback() {
		delegate.rollback();
	}

	@Override
	public LocalStatus getLocalStatus() {
		return delegate.getLocalStatus();
	}

	@Override
	public boolean isActive() {
		return delegate.isActive();
	}

	@Override
	public boolean isParticipating() {
		return delegate.isParticipating();
	}

	@Override
	public boolean wasCommitted() {
		return delegate.wasCommitted();
	}

	@Override
	public boolean wasRolledBack() {
		return delegate.wasRolledBack();
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) throws HibernateException {
		delegate.registerSynchronization( synchronization );
	}

	@Override
	public void setTimeout(int seconds) {
		delegate.setTimeout( seconds );
	}

	@Override
	public int getTimeout() {
		return delegate.getTimeout();
	}

	TransactionImplementor getDelegate() {
		return delegate;
	}
}
