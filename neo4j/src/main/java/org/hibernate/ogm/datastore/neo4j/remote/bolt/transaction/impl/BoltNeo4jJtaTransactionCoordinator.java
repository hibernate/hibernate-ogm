/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.transaction.impl;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.ogm.datastore.neo4j.remote.bolt.impl.BoltNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.impl.BoltNeo4jDatastoreProvider;
import org.hibernate.ogm.dialect.impl.IdentifiableDriver;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinator;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionDriver;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;

/**
 * A {@link TransactionCoordinator} for a remote Neo4j.
 *
 * Note that during a JTA transaction, Neo4j {@link Transaction} are
 * synchronized using the {@link Synchronization} interface. A commit to the Neo4j transaction will happen before the
 * end of the JTA transaction, meaning that it won't be possible to roll-back if an error happen after successful commit
 * to the db.
 *
 * @author Davide D'Alto
 */
public class BoltNeo4jJtaTransactionCoordinator extends ForwardingTransactionCoordinator {

	private final Driver driver;
	private Transaction tx;
	private Session session;

	public BoltNeo4jJtaTransactionCoordinator(TransactionCoordinator delegate, BoltNeo4jDatastoreProvider provider) {
		super( delegate );
		this.driver = ( (BoltNeo4jClient) provider.getClient() ).getDriver();
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
			session = driver.session();
			tx = session.beginTransaction();
			delegate.getLocalSynchronizations().registerSynchronization( new Neo4jSynchronization() );
		}
	}

	private void success() {
		if ( tx != null ) {
			try {
				tx.success();
				tx.close();
			}
			finally {
				tx = null;
				closeSession();
			}
		}
	}

	private void failure() {
		if ( tx != null ) {
			try {
				tx.failure();
				tx.close();
			}
			finally {
				tx = null;
				closeSession();
			}
		}
	}

	private void closeSession() {
		try {
			session.close();
		}
		finally {
			session = null;
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
		public Transaction getTransactionId() {
			return tx;
		}

		@Override
		public void begin() {
			super.begin();
			if ( session == null ) {
				session = driver.session();
			}
			if ( tx == null ) {
				tx = session.beginTransaction();
			}
		}

		@Override
		public void commit() {
			try {
				try {
					super.commit();
					success();
				}
				catch (Exception e) {
					try {
						failure();
					}
					catch (Exception re) {
					}
					throw e;
				}
			}
			finally {
				closeSession();
			}
		}

		@Override
		public void rollback() {
			try {
				try {
					super.rollback();
				}
				finally {
					failure();
				}
			}
			finally {
				closeSession();
			}
		}
	}
}
