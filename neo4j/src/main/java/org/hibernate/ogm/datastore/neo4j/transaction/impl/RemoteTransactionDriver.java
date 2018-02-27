/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.transaction.impl;

import org.hibernate.ogm.dialect.impl.IdentifiableDriver;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionDriver;
import org.hibernate.resource.transaction.spi.TransactionCoordinator.TransactionDriver;

/**
 * {@link IdentifiableDriver} implementation for Neo4j.
 *
 * @author Davide D'Alto
 */
public class RemoteTransactionDriver extends ForwardingTransactionDriver implements IdentifiableDriver {

	private final BaseNeo4jJtaTransactionCoordinator delegate;

	public RemoteTransactionDriver(BaseNeo4jJtaTransactionCoordinator delegate, TransactionDriver transactionDriver) {
		super( transactionDriver );
		this.delegate = delegate;
	}

	@Override
	public Object getTransactionId() {
		return delegate.getTransactionId();
	}

	@Override
	public void begin() {
		super.begin();
		delegate.beginTransaction();
	}

	@Override
	public void commit() {
		try {
			super.commit();
			delegate.success();
		}
		catch (Exception e) {
			try {
				delegate.failure();
			}
			catch ( Exception re ) {
			}
			throw e;
		}
	}

	@Override
	public void rollback() {
		try {
			super.rollback();
		}
		finally {
			delegate.failure();
		}
	}
}
