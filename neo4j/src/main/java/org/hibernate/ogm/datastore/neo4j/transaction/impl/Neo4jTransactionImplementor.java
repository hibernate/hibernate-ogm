/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.transaction.impl;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionImplementor;
import org.neo4j.graphdb.Transaction;

/**
 * @author Davide D'Alto
 */
public class Neo4jTransactionImplementor extends ForwardingTransactionImplementor {

	private static final Log log = LoggerFactory.getLogger();

	private final Neo4jDatastoreProvider datastoreProvider;
	private final JtaPlatform jtaPlatform;
	private Transaction tx = null;

	public Neo4jTransactionImplementor(TransactionImplementor delegate, Neo4jDatastoreProvider datastoreProvider, JtaPlatform jtaPlatform) {
		super( delegate );
		this.datastoreProvider = datastoreProvider;
		this.jtaPlatform = jtaPlatform;
	}

	@Override
	public void begin() {
		super.begin();
		beginNeo4jTransaction();
	}

	private void beginNeo4jTransaction() {
		tx = datastoreProvider.getDataBase().beginTx();
	}

	@Override
	public void commit() {
		try {
			super.commit();
		}
		finally {
			if ( super.wasCommitted() ) {
				success();
			}
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

	@Override
	public void join() {
		boolean jtaTransactionActive = jtaPlatform != null && JtaStatusHelper.isActive( jtaPlatform.retrieveTransactionManager() );

		if ( jtaTransactionActive && tx == null ) {
			beginNeo4jTransaction();
			jtaPlatform.registerSynchronization( new Neo4jSynchronization() );
		}

		super.join();
	}

	@Override
	protected TransactionImplementor getDelegate() {
		return super.getDelegate();
	}

	private void success() {
		try {
			tx.success();
		}
		finally {
			close();
		}
	}

	private void failure() {
		try {
			tx.failure();
		}
		finally {
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

	private class Neo4jSynchronization implements Synchronization {

		@Override
		public void beforeCompletion() {
			boolean markedForRollback = markedForRollback();
			if ( !markedForRollback ) {
				success();
			}
		}

		private boolean markedForRollback() {
			boolean markedForRollback = true;
			TransactionManager transactionManager = jtaPlatform.retrieveTransactionManager();
			if ( transactionManager != null ) {
				try {
					if ( transactionManager.getTransaction() != null ) {
						markedForRollback = ( Status.STATUS_MARKED_ROLLBACK == transactionManager.getTransaction().getStatus() );
					}
					return markedForRollback;
				}
				catch (SystemException e) {
					throw log.exceptionWhileChekingTransactionStatus( e );
				}
			}
			return markedForRollback;
		}

		@Override
		public void afterCompletion(int status) {
			if ( Status.STATUS_COMMITTED != status ) {
				if ( tx != null ) {
					failure();
				}
			}
		}

	}
}
