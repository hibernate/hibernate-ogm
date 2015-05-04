/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.impl;

import org.hibernate.resource.transaction.TransactionCoordinator.LocalInflow;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * A {@link LocalInflow} forwarding all invocations to a delegate.
 *
 * @author Gunnar Morling
 */
public class ForwardingLocalInflow implements LocalInflow {

	private final LocalInflow delegate;

	public ForwardingLocalInflow(LocalInflow delegate) {
		this.delegate = delegate;
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
	public TransactionStatus getStatus() {
		return delegate.getStatus();
	}

	@Override
	public void markRollbackOnly() {
		delegate.markRollbackOnly();
	}
}
