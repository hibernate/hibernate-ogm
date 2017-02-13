/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.transaction.impl;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE;
import com.orientechnologies.orient.core.tx.OTransactionOptimistic;
import org.hibernate.ogm.datastore.orientdb.impl.OrientDBDatastoreProvider;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinator;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionDriver;
import org.hibernate.resource.transaction.TransactionCoordinator;

/**
 * Coordinator for local transactions
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDBLocalTransactionCoordinator extends ForwardingTransactionCoordinator {

	private static Log log = LoggerFactory.getLogger();
	private final OrientDBDatastoreProvider datastoreProvider;
	private OTransactionOptimistic currentOrientDBTransaction;

	/**
	 * Constructor
	 *
	 * @param coordinator transaction coordinator
	 * @param datastoreProvider provider of OrientDB datastore
	 */
	public OrientDBLocalTransactionCoordinator(TransactionCoordinator coordinator, OrientDBDatastoreProvider datastoreProvider) {
		super( coordinator );
		this.datastoreProvider = datastoreProvider;
	}

	@Override
	public TransactionDriver getTransactionDriverControl() {
		TransactionDriver driver = super.getTransactionDriverControl();
		return new OrientDBTransactionDriver( driver );
	}

	private void success() {
		if ( currentOrientDBTransaction != null && currentOrientDBTransaction.isActive() ) {
			log.debugf( "commit transaction (Id: %d) for database %s is %d.",
					currentOrientDBTransaction.getId(),
					currentOrientDBTransaction.getDatabase().getName() );
			log.debugf( "transaction state: %s", currentOrientDBTransaction );
			currentOrientDBTransaction.commit();
			close();
		}
		else {
			currentOrientDBTransaction = null;
		}
	}

	private void failure() {
		if ( currentOrientDBTransaction != null && currentOrientDBTransaction.isActive() ) {
			log.debugf( "1.rollback transaction (Id: %d) for database %s is %d.",
					currentOrientDBTransaction.getId(),
					currentOrientDBTransaction.getDatabase().getName() );
			log.debugf( "transaction state: %s", currentOrientDBTransaction );
			currentOrientDBTransaction.rollback();
			close();
		}
		else {
			currentOrientDBTransaction = null;
		}
	}

	private void close() {
		try {
			log.debugf( "close connection for thread %s", Thread.currentThread().getName() );
			datastoreProvider.closeCurrentDatabase();
		}
		finally {
			currentOrientDBTransaction = null;
		}
	}

	private class OrientDBTransactionDriver extends ForwardingTransactionDriver {

		public OrientDBTransactionDriver(TransactionDriver delegate) {
			super( delegate );
		}

		@Override
		public void begin() {
			ODatabaseDocumentTx database = datastoreProvider.getCurrentDatabase();
			log.debugf( "begin transaction for database %s. Connection's hash code: %s",
					database.getName(), database.hashCode() );
			super.begin();

			currentOrientDBTransaction = (OTransactionOptimistic) database.activateOnCurrentThread()
					.begin( TXTYPE.OPTIMISTIC ).getTransaction();
			currentOrientDBTransaction.setUsingLog( true );
			log.debugf( "Id of current transaction for database %s  is %d. (transaction: %s)", database.getName(),
					currentOrientDBTransaction.getId(), currentOrientDBTransaction );
		}

		@Override
		public void commit() {
			try {
				if ( currentOrientDBTransaction != null && currentOrientDBTransaction.isActive() ) {
					log.debugf( "commit transaction N %s for database %s. Transaction acvite? %s",
							String.valueOf( currentOrientDBTransaction.getId() ),
							currentOrientDBTransaction.getDatabase().getName(),
							String.valueOf( currentOrientDBTransaction.isActive() ) );
					log.debugf( "transaction state: %s", currentOrientDBTransaction );
					super.commit();
					success();

				}

			}
			catch (Exception e) {
				log.error( "Cannot commit transaction!", e );
				try {
					rollback();
				}
				catch (Exception re) {
				}
				throw e;
			}
		}

		@Override
		public void rollback() {
			try {
				if ( currentOrientDBTransaction != null && currentOrientDBTransaction.isActive() ) {
					log.debugf( "2.rollback  transaction N %s for database %s. Transaction acvite? %s",
							String.valueOf( currentOrientDBTransaction.getId() ),
							currentOrientDBTransaction.getDatabase().getName(),
							String.valueOf( currentOrientDBTransaction.isActive() ) );
					log.debugf( "transaction state: %s", currentOrientDBTransaction );
					currentOrientDBTransaction.rollback( true, 0 );
					super.rollback();
				}
			}
			finally {
				failure();
			}
		}
	}

}
