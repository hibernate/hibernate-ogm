/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.transaction.impl;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.ogm.datastore.neo4j.remote.impl.Neo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jDatastoreProvider;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinator;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionDriver;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * @author Davide D'Alto
 */
public class RemoteJtaTransactionCoordinator extends ForwardingTransactionCoordinator {

	private final Neo4jClient remoteNeo4j;
	private Transaction tx;

	public RemoteJtaTransactionCoordinator(TransactionCoordinator delegate, RemoteNeo4jDatastoreProvider provider) {
		super( delegate );
		this.remoteNeo4j = provider.getDataStore();
	}

	public long getTransactionId() {
		return tx.getId();
	}

	@Override
	public TransactionDriver getTransactionDriverControl() {
		TransactionDriver driver = super.getTransactionDriverControl();
		return new Neo4jTransactionDriver( driver );
	}

	@Override
	public void explicitJoin() {
		super.explicitJoin();
		join();
	}

	@Override
	public void pulse() {
		super.pulse();
		join();
	}

	private void join() {
		if ( tx == null && delegate.isActive() && delegate.getTransactionCoordinatorBuilder().isJta() ) {
			tx = remoteNeo4j.beginTx();
			delegate.getLocalSynchronizations().registerSynchronization( new Neo4jSynchronization() );
		}
	}

	private void success() {
		if ( tx != null ) {
			tx.commit();
			tx = null;
		}
	}

	private void failure() {
		if ( tx != null ) {
			tx.rollback();
			tx = null;
		}
	}

	private class Neo4jSynchronization implements Synchronization {

		@Override
		public void beforeCompletion() {
			TransactionStatus status = delegate.getTransactionDriverControl().getStatus();
			if ( status == TransactionStatus.MARKED_ROLLBACK ) {
				failure();
			}
			else {
				success();
			}
		}

		@Override
		public void afterCompletion(int status) {
			if ( tx != null ) {
				if ( status != Status.STATUS_COMMITTED ) {
					failure();
				}
				else {
					success();
				}
			}
		}
	}

	private class Neo4jTransactionDriver extends ForwardingTransactionDriver {

		public Neo4jTransactionDriver(TransactionDriver delegate) {
			super( delegate );
		}

		@Override
		public void begin() {
			super.begin();
			if ( tx == null ) {
				tx = remoteNeo4j.beginTx();
			}
		}

		@Override
		public void commit() {
			try {
				super.commit();
				success();
			}
			catch (Exception e) {
				try {
					failure();
				}
				catch (Exception rollbackEx) {
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
				failure();
			}
		}

	}
}
