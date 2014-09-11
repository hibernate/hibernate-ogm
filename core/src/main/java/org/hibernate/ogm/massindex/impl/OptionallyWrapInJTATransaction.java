/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.massindex.impl;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Wrap the subsequent Runnable in a JTA Transaction if necessary:
 * - if the existing Hibernate Core transaction strategy requires a TransactionManager
 * - if no JTA transaction is already started
 *
 * Unfortunately at this time we need to have access to SessionFactoryImplementor
 *
 * @author Emmanuel Bernard
 */
public class OptionallyWrapInJTATransaction implements ModelConsumer {

	private static final Log log = LoggerFactory.make();

	private final SessionFactoryImplementor factory;
	private final SessionAwareRunnable delegate;
	private final ErrorHandler errorHandler;

	public OptionallyWrapInJTATransaction(SessionFactory factory, ErrorHandler errorHandler,
			SessionAwareRunnable sessionAwareRunnable) {
		/*
		 * Unfortunately we need to access SessionFactoryImplementor to detect:
		 * - whether or not we need to start the JTA transaction
		 * - start it
		 */
		// TODO get SessionFactoryImplementor it from the SearchFactory as we might get a hold of the SFI at startup
		// time
		// if that's the case, SearchFactory should expose something like T unwrap(Class<T> clazz);
		this.factory = (SessionFactoryImplementor) factory;
		this.delegate = sessionAwareRunnable;
		this.errorHandler = errorHandler;
	}

	private TransactionManager getTransactionManager() {
		return factory.getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager();
	}

	boolean wrapInTransaction() {
		final TransactionFactory transactionFactory = factory.getServiceRegistry()
				.getService( TransactionFactory.class );
		if ( !transactionFactory.compatibleWithJtaSynchronization() ) {
			// Today we only require a TransactionManager on JTA based transaction factories
			log.trace( "TransactionFactory does not require a TransactionManager: don't wrap in a JTA transaction" );
			return false;
		}
		final TransactionManager transactionManager = getTransactionManager();
		if ( transactionManager == null ) {
			// no TM, nothing to do OR configuration mistake
			log.trace( "No TransactionManager found, do not start a surrounding JTA transaction" );
			return false;
		}
		try {
			if ( transactionManager.getStatus() == Status.STATUS_NO_TRANSACTION ) {
				log.trace( "No Transaction in progress, needs to start a JTA transaction" );
				return true;
			}
		}
		catch ( SystemException e ) {
			log.cannotGuessTransactionStatus( e );
			return false;
		}
		log.trace( "Transaction in progress, no needs to start a JTA transaction" );
		return false;
	}

	@Override
	public void consume(Tuple tuple) {
		try {
			final boolean wrapInTransaction = wrapInTransaction();
			if ( wrapInTransaction ) {
				consumeInTransaction( tuple );
			}
			else {
				delegate.run( null, tuple );
			}
		}
		catch ( Throwable e ) {
			errorHandler.handleException( log.massIndexerUnexpectedErrorMessage(), e );
		}
	}

	private void consumeInTransaction(Tuple tuple) {
		TransactionManager transactionManager = getTransactionManager();
		try {
			final Session session = factory.openSession();
			transactionManager.begin();
			delegate.run( session, tuple );
			transactionManager.commit();
			session.close();
		}
		catch ( Throwable e ) {
			errorHandler.handleException( log.massIndexerUnexpectedErrorMessage(), e );
			rollback( transactionManager, e );
		}
	}

	private void rollback(TransactionManager transactionManager, Throwable e) {
		try {
			transactionManager.rollback();
		}
		catch ( SystemException e1 ) {
			// we already have an exception, don't propagate this one
			log.errorRollingBackTransaction( e.getMessage(), e1 );
		}
	}
}
