/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.transaction.impl;

import javax.transaction.Synchronization;

import org.hibernate.ogm.datastore.neo4j.remote.impl.Neo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jDatastoreProvider;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinator;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionDriver;
import org.hibernate.resource.transaction.TransactionCoordinator;

/**
 * A {@link TransactionCoordinator} for Neo4j.
 *
 * Note that during a JTA transaction Neo4j {@link Transaction} are
 * synchronized using the {@link Synchronization} interface. A commit to the Neo4j transaction will happen before the
 * end of the JTA transaction, meaning that it won't be possible to rollback if an error happen after succesful commit
 * to the db.
 *
 * @author Davide D'Alto
 */
public class RemoteLocalTransactionCoordinator extends ForwardingTransactionCoordinator {

	private final Neo4jClient graphDB;
	private Transaction tx = null;

	public RemoteLocalTransactionCoordinator(TransactionCoordinator delegate, RemoteNeo4jDatastoreProvider graphDb) {
		super( delegate );
		this.graphDB = graphDb.getDataStore();
	}

	@Override
	public TransactionDriver getTransactionDriverControl() {
		TransactionDriver driver = super.getTransactionDriverControl();
		return new RemoteTransactionDriver( driver );
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

	private class RemoteTransactionDriver extends ForwardingTransactionDriver implements Neo4jTransactionDriver {

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
				tx = graphDB.beginTx();
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
					rollback();
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
