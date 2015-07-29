/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.impl;

import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * A {@link TransactionCoordinatorOwner} forwarding all invocations to a delegate.
 *
 * @author Gunnar Morling
 */
public class ForwardingTransactionCoordinatorOwner implements TransactionCoordinatorOwner {

	private final TransactionCoordinatorOwner delegate;

	public ForwardingTransactionCoordinatorOwner(TransactionCoordinatorOwner delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean isActive() {
		return delegate.isActive();
	}

	@Override
	public void afterTransactionBegin() {
		delegate.afterTransactionBegin();
	}

	@Override
	public void beforeTransactionCompletion() {
		delegate.beforeTransactionCompletion();
	}

	@Override
	public void afterTransactionCompletion(boolean successful, boolean delayed) {
		delegate.afterTransactionCompletion( successful, delayed );
	}

	@Override
	public JdbcSessionOwner getJdbcSessionOwner() {
		return delegate.getJdbcSessionOwner();
	}

	@Override
	public void setTransactionTimeOut(int seconds) {
		delegate.setTransactionTimeOut( seconds );
	}

	@Override
	public void flushBeforeTransactionCompletion() {
		delegate.flushBeforeTransactionCompletion();
	}
}
