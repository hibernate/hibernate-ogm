/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.transaction.impl;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.transaction.Status;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionObserver;
import org.hibernate.jdbc.WorkExecutor;
import org.hibernate.jdbc.WorkExecutorVisitable;
import org.hibernate.ogm.datastore.neo4j.embedded.impl.EmbeddedNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.resource.transaction.internal.SynchronizationRegistryStandardImpl;
import org.hibernate.resource.transaction.spi.SynchronizationRegistry;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

/**
 * An implementation of TransactionCoordinator based on managing a transaction through a Neo4j Connection.
 * <p>
 * This is inspired by {@code org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImp}.
 *
 * @author Davide D'Alto
 */
public class EmbeddedNeo4jResourceLocalTransactionCoordinator implements TransactionCoordinator {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final TransactionCoordinatorBuilder transactionCoordinatorBuilder;
	private final TransactionCoordinatorOwner owner;
	private final SynchronizationRegistryStandardImpl synchronizationRegistry = new SynchronizationRegistryStandardImpl();

	private Neo4jTransactionDriver physicalTransactionDelegate;

	private int timeOut = -1;

	private final transient List<TransactionObserver> observers;

	private final EmbeddedNeo4jDatastoreProvider provider;

	/**
	 * Construct a ResourceLocalTransactionCoordinatorImpl instance. Package-protected to ensure access goes through
	 * builder.
	 *
	 * @param owner The transactionCoordinatorOwner
	 */
	EmbeddedNeo4jResourceLocalTransactionCoordinator(
			TransactionCoordinatorBuilder transactionCoordinatorBuilder,
			TransactionCoordinatorOwner owner,
			EmbeddedNeo4jDatastoreProvider provider) {
		this.provider = provider;
		this.observers = new ArrayList<>();
		this.transactionCoordinatorBuilder = transactionCoordinatorBuilder;
		this.owner = owner;
	}

	@Override
	public TransactionDriver getTransactionDriverControl() {
		// Again, this PhysicalTransactionDelegate will act as the bridge from the local transaction back into the
		// coordinator. We lazily build it as we invalidate each delegate after each transaction (a delegate is
		// valid for just one transaction)
		if ( physicalTransactionDelegate == null ) {
			physicalTransactionDelegate = new Neo4jTransactionDriver( provider );
		}
		return physicalTransactionDelegate;
	}

	@Override
	public void explicitJoin() {
		// nothing to do here, but log a warning
		log.callingJoinTransactionOnNonJtaEntityManager();
	}

	@Override
	public boolean isJoined() {
		return physicalTransactionDelegate != null && physicalTransactionDelegate.getStatus() == TransactionStatus.ACTIVE;
	}

	@Override
	public void pulse() {
		getTransactionDriverControl();
	}

	@Override
	public SynchronizationRegistry getLocalSynchronizations() {
		return synchronizationRegistry;
	}

	@Override
	public boolean isActive() {
		return owner.isActive();
	}

	@Override
	public IsolationDelegate createIsolationDelegate() {
		return new Neo4jIsolationDelegate( provider );
	}

	private class Neo4jIsolationDelegate implements IsolationDelegate {

		private final EmbeddedNeo4jDatastoreProvider provider;

		public Neo4jIsolationDelegate(EmbeddedNeo4jDatastoreProvider provider) {
			this.provider = provider;
		}

		@Override
		public <T> T delegateWork(WorkExecutorVisitable<T> work, boolean transacted) throws HibernateException {
			Transaction tx = null;
			try {
				if ( !transacted ) {
					log.cannotExecuteWorkOutsideIsolatedTransaction();
				}
				GraphDatabaseService dataBase = provider.getDatabase();
				tx = dataBase.beginTx();
				// Neo4j does not have a connection object, I'm not sure what it is best to do in this case.
				// In this scenario I expect the visitable object to already have a way to connect to the db.
				Connection connection = null;
				T result = work.accept( new WorkExecutor<T>(), connection );
				tx.success();
				return result;
			}
			catch (Exception e) {
				try {
					tx.failure();
				}
				catch (Exception re) {
					log.unableToRollbackTransaction( re );
				}
				if ( e instanceof HibernateException ) {
					throw (HibernateException) e;
				}
				else {
					throw log.unableToPerformIsolatedWork( e );
				}
			}
			finally {
				if ( tx != null ) {
					tx.close();
					tx = null;
				}
			}
		}

		@Override
		public <T> T delegateCallable(Callable<T> callable, boolean transacted) throws HibernateException {
			throw new UnsupportedOperationException( "Not implemented yet" );
		}
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder() {
		return this.transactionCoordinatorBuilder;
	}

	@Override
	public void setTimeOut(int seconds) {
		this.timeOut = seconds;
	}

	@Override
	public int getTimeOut() {
		return this.timeOut;
	}

	// PhysicalTransactionDelegate ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void afterBeginCallback() {
		if ( this.timeOut > 0 ) {
			owner.setTransactionTimeOut( this.timeOut );
		}
		owner.afterTransactionBegin();
		for ( TransactionObserver observer : observers ) {
			observer.afterBegin();
		}
		log.trace( "ResourceLocalTransactionCoordinatorImpl#afterBeginCallback" );
	}

	private void beforeCompletionCallback() {
		log.trace( "ResourceLocalTransactionCoordinatorImpl#beforeCompletionCallback" );
		try {
			owner.beforeTransactionCompletion();
			synchronizationRegistry.notifySynchronizationsBeforeTransactionCompletion();
			for ( TransactionObserver observer : observers ) {
				observer.beforeCompletion();
			}
		}
		catch (RuntimeException e) {
			if ( physicalTransactionDelegate != null ) {
				// should never happen that the physicalTransactionDelegate is null, but to be safe
				physicalTransactionDelegate.markRollbackOnly();
			}
			throw e;
		}
	}

	private void afterCompletionCallback(boolean successful) {
		log.tracef( "ResourceLocalTransactionCoordinatorImpl#afterCompletionCallback(%s)", successful );
		final int statusToSend = successful ? Status.STATUS_COMMITTED : Status.STATUS_UNKNOWN;
		synchronizationRegistry.notifySynchronizationsAfterTransactionCompletion( statusToSend );

		owner.afterTransactionCompletion( successful, false );
		for ( TransactionObserver observer : observers ) {
			observer.afterCompletion( successful, false );
		}
		invalidateDelegate();
	}

	private void invalidateDelegate() {
		if ( physicalTransactionDelegate == null ) {
			throw new IllegalStateException( "Physical-transaction delegate not known on attempt to invalidate" );
		}

		physicalTransactionDelegate.invalidate();
		physicalTransactionDelegate = null;
	}

	@Override
	public void addObserver(TransactionObserver observer) {
		observers.add( observer );
	}

	@Override
	public void removeObserver(TransactionObserver observer) {
		observers.remove( observer );
	}

	/**
	 * The delegate bridging between the local (application facing) transaction and the "physical" notion of a
	 * transaction via the {@link GraphDatabaseService}.
	 */
	public class Neo4jTransactionDriver implements TransactionDriver {

		private final GraphDatabaseService graphDB;
		private TransactionStatus status;
		private Transaction tx;

		private boolean invalid;
		private boolean rollbackOnly = false;

		public Neo4jTransactionDriver(EmbeddedNeo4jDatastoreProvider provider) {
			this.graphDB = provider.getDatabase();
		}

		protected void invalidate() {
			invalid = true;
		}

		@Override
		public void begin() {
			errorIfInvalid();
			tx = graphDB.beginTx();
			status = TransactionStatus.ACTIVE;
			EmbeddedNeo4jResourceLocalTransactionCoordinator.this.afterBeginCallback();
		}

		protected void errorIfInvalid() {
			if ( invalid ) {
				throw new IllegalStateException( "Physical-transaction delegate is no longer valid" );
			}
		}

		@Override
		public void commit() {
			try {
				if ( rollbackOnly ) {
					throw new TransactionException( "Transaction was marked for rollback only; cannot commit" );
				}

				EmbeddedNeo4jResourceLocalTransactionCoordinator.this.beforeCompletionCallback();
				tx.success();
				close();
				status = TransactionStatus.NOT_ACTIVE;
				EmbeddedNeo4jResourceLocalTransactionCoordinator.this.afterCompletionCallback( true );
			}
			catch (RuntimeException e) {
				try {
					rollback();
				}
				catch (RuntimeException e2) {
					log.unableToRollbackTransaction( e2 );
				}
				throw e;
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
		public void rollback() {
			if ( rollbackOnly || getStatus() == TransactionStatus.ACTIVE ) {
				rollbackOnly = false;
				tx.failure();
				status = TransactionStatus.NOT_ACTIVE;
				close();
				EmbeddedNeo4jResourceLocalTransactionCoordinator.this.afterCompletionCallback( false );
			}

			// no-op otherwise.
		}

		@Override
		public TransactionStatus getStatus() {
			return rollbackOnly ? TransactionStatus.MARKED_ROLLBACK : status;
		}

		@Override
		public void markRollbackOnly() {
			if ( log.isDebugEnabled() ) {
				log.debug(
						"Neo4j transaction marked for rollback-only (exception provided for stack trace)",
						new Exception( "exception just for purpose of providing stack trace" ) );
			}

			rollbackOnly = true;
		}
	}
}
