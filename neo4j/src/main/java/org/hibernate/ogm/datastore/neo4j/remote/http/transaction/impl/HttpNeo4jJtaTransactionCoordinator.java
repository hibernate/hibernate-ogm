/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.transaction.impl;

import javax.transaction.Synchronization;

import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.transaction.impl.BaseNeo4jJtaTransactionCoordinator;
import org.hibernate.ogm.datastore.neo4j.transaction.impl.Neo4jSynchronization;
import org.hibernate.ogm.datastore.neo4j.transaction.impl.RemoteTransactionDriver;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;

/**
 * A {@link TransactionCoordinator} for a remote Neo4j.
 *
 * Note that during a JTA transaction Neo4j {@link HttpNeo4jTransaction} are
 * synchronized using the {@link Synchronization} interface. A commit to the Neo4j transaction will happen before the
 * end of the JTA transaction, meaning that it won't be possible to roll-back if an error happen after successful commit
 * to the db.
 *
 * @author Davide D'Alto
 */
public class HttpNeo4jJtaTransactionCoordinator extends ForwardingTransactionCoordinator implements BaseNeo4jJtaTransactionCoordinator {

	private final HttpNeo4jClient remoteNeo4j;
	private HttpNeo4jTransaction tx;

	public HttpNeo4jJtaTransactionCoordinator(TransactionCoordinator delegate, HttpNeo4jDatastoreProvider provider) {
		super( delegate );
		this.remoteNeo4j = provider.getClient();
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
	public void success() {
		if ( tx != null ) {
			tx.commit();
			tx = null;
		}
	}

	@Override
	public void failure() {
		if ( tx != null ) {
			tx.rollback();
			tx = null;
		}
	}

	@Override
	public boolean isTransactionOpen() {
		return tx != null;
	}

	@Override
	public Object getTransactionId() {
		if ( tx == null ) {
			return null;
		}
		return tx.getId();
	}

	@Override
	public void beginTransaction() {
		tx = remoteNeo4j.beginTx();
	}
}
