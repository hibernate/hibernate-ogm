/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.transaction.impl;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jDatastoreProvider;
import org.hibernate.ogm.dialect.impl.IdentifiableDriver;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinator;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionDriver;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * A {@link TransactionCoordinator} for a remote Neo4j.
 *
 * Note that during a JTA transaction Neo4j {@link RemoteNeo4jTransaction} are
 * synchronized using the {@link Synchronization} interface. A commit to the Neo4j transaction will happen before the
 * end of the JTA transaction, meaning that it won't be possible to roll-back if an error happen after successful commit
 * to the db.
 *
 * @author Davide D'Alto
 */
public class RemoteNeo4jJtaTransactionCoordinator extends ForwardingTransactionCoordinator {

	private final RemoteNeo4jClient remoteNeo4j;
	private RemoteNeo4jTransaction tx;

	public RemoteNeo4jJtaTransactionCoordinator(TransactionCoordinator delegate, RemoteNeo4jDatastoreProvider provider) {
		super( delegate );
		this.remoteNeo4j = provider.getDatabase();
	}

	@Override
	public TransactionDriver getTransactionDriverControl() {
		TransactionDriver driver = super.getTransactionDriverControl();
		return new RemoteTransactionDriver( driver );
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

	private class RemoteTransactionDriver extends ForwardingTransactionDriver implements IdentifiableDriver {

		public RemoteTransactionDriver(TransactionDriver delegate) {
			super( delegate );
		}

		@Override
		public Long getTransactionId() {
			if ( tx == null ) {
				return null;
			}
			return tx.getId();
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
				failure();
			}
		}
	}
}
