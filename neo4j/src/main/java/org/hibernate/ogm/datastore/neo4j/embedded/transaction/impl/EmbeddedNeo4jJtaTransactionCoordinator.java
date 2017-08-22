/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.transaction.impl;

import javax.transaction.Synchronization;

import org.hibernate.ogm.datastore.neo4j.embedded.impl.EmbeddedNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.transaction.impl.BaseNeo4jJtaTransactionCoordinator;
import org.hibernate.ogm.datastore.neo4j.transaction.impl.Neo4jSynchronization;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

/**
 * A {@link TransactionCoordinator} for Neo4j to join a JTA transaction.
 * <p>
 * Note that Neo4j {@link Transaction}s are synchronized using the {@link Synchronization} interface.
 * A commit to the Neo4j transaction will happen before the end of the JTA transaction,
 * meaning that it won't be possible to rollback if an error happens after a successful commit
 * to the db.
 *
 * @author Davide D'Alto
 */
public class EmbeddedNeo4jJtaTransactionCoordinator extends ForwardingTransactionCoordinator implements BaseNeo4jJtaTransactionCoordinator {

	private final GraphDatabaseService graphDB;
	private Transaction tx;

	public EmbeddedNeo4jJtaTransactionCoordinator(TransactionCoordinator jtaDelegate, EmbeddedNeo4jDatastoreProvider graphDb) {
		super( jtaDelegate );
		this.graphDB = graphDb.getDatabase();
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
		if ( tx == null && isActive() ) {
			beginTransaction();
			getLocalSynchronizations().registerSynchronization( new Neo4jSynchronization( this ) );
		}
	}

	@Override
	public void success() {
		if ( tx != null ) {
			tx.success();
			close();
		}
	}

	@Override
	public void failure() {
		if ( tx != null ) {
			tx.failure();
			close();
		}
	}

	private void close() {
		try {
			tx.close();
		}
		finally {
			tx = null;
		}
	}

	@Override
	public boolean isTransactionOpen() {
		return tx != null;
	}

	@Override
	public void beginTransaction() {
		tx = graphDB.beginTx();
	}

	@Override
	public Object getTransactionId() {
		return tx;
	}
}
