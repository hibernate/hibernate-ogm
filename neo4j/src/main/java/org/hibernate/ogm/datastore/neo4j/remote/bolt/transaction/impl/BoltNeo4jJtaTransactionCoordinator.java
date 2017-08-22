/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.transaction.impl;

import javax.transaction.Synchronization;

import org.hibernate.ogm.datastore.neo4j.remote.bolt.impl.BoltNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.impl.BoltNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.transaction.impl.BaseNeo4jJtaTransactionCoordinator;
import org.hibernate.ogm.datastore.neo4j.transaction.impl.Neo4jSynchronization;
import org.hibernate.ogm.datastore.neo4j.transaction.impl.RemoteTransactionDriver;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
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
public class BoltNeo4jJtaTransactionCoordinator extends ForwardingTransactionCoordinator implements BaseNeo4jJtaTransactionCoordinator {

	private final Driver driver;
	private Transaction tx;
	private Session session;

	public BoltNeo4jJtaTransactionCoordinator(TransactionCoordinator delegate, BoltNeo4jDatastoreProvider provider) {
		super( delegate );
		this.driver = ( (BoltNeo4jClient) provider.getClient() ).getDriver();
	}

	@Override
	public TransactionDriver getTransactionDriverControl() {
		return new RemoteTransactionDriver( this, super.getTransactionDriverControl() );
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

	@Override
	public void join() {
		if ( tx == null && delegate.isActive() && delegate.getTransactionCoordinatorBuilder().isJta() ) {
			beginTransaction();
			delegate.getLocalSynchronizations().registerSynchronization( new Neo4jSynchronization( this ) );
		}
	}

	@Override
	public void beginTransaction() {
		if ( session == null ) {
			session = driver.session();
		}
		if ( tx == null ) {
			tx = session.beginTransaction();
		}
	}

	@Override
	public void success() {
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

	@Override
	public void failure() {
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

	@Override
	public boolean isTransactionOpen() {
		return tx != null;
	}

	@Override
	public Object getTransactionId() {
		return tx;
	}
}
