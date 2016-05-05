/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.transaction.impl;

import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinator;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionDriver;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

/**
 * A {@link TransactionCoordinator} that allows the management of a local transaction with Neo4j.
 *
 * @author Davide D'Alto
 */
public class Neo4jLocalTransactionCoordinator extends ForwardingTransactionCoordinator {

	private final GraphDatabaseService graphDB;
	private Transaction tx = null;

	public Neo4jLocalTransactionCoordinator(TransactionCoordinator delegate, Neo4jDatastoreProvider graphDb) {
		super( delegate );
		this.graphDB = graphDb.getDataBase();
	}

	@Override
	public TransactionDriver getTransactionDriverControl() {
		TransactionDriver driver = super.getTransactionDriverControl();
		return new Neo4jTransactionDriver( driver );
	}

	private void success() {
		if ( tx != null ) {
			tx.success();
			close();
		}
	}

	private void failure() {
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

	private class Neo4jTransactionDriver extends ForwardingTransactionDriver {

		public Neo4jTransactionDriver(TransactionDriver delegate) {
			super( delegate );
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
