/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.impl;

import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.resource.transaction.spi.SynchronizationRegistry;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;

/**
 * A {@link TransactionCoordinator} forwarding all invocations to a delegate.
 *
 * @author Gunnar Morling
 */
public class ForwardingTransactionCoordinator implements TransactionCoordinator {

	protected final TransactionCoordinator delegate;

	public ForwardingTransactionCoordinator(TransactionCoordinator delegate) {
		this.delegate = delegate;
	}

	@Override
	public void explicitJoin() {
		delegate.explicitJoin();
	}

	@Override
	public boolean isJoined() {
		return delegate.isJoined();
	}

	@Override
	public void pulse() {
		delegate.pulse();
	}

	@Override
	public TransactionDriver getTransactionDriverControl() {
		return delegate.getTransactionDriverControl();
	}

	@Override
	public SynchronizationRegistry getLocalSynchronizations() {
		return delegate.getLocalSynchronizations();
	}

	@Override
	public boolean isActive() {
		return delegate.isActive();
	}

	@Override
	public IsolationDelegate createIsolationDelegate() {
		return delegate.createIsolationDelegate();
	}

	@Override
	public void addObserver(TransactionObserver observer) {
		delegate.addObserver( observer );
	}

	@Override
	public void removeObserver(TransactionObserver observer) {
		delegate.removeObserver( observer );
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder() {
		return delegate.getTransactionCoordinatorBuilder();
	}

	@Override
	public void setTimeOut(int seconds) {
		delegate.setTimeOut( seconds );
	}

	@Override
	public int getTimeOut() {
		return delegate.getTimeOut();
	}
}
