/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.impl;

import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.TransactionException;
import org.hibernate.engine.transaction.internal.jta.JtaStatusHelper;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionImplementor;

/**
 * TransactionFactory using JTA transactions exclusively from the TransactionManager
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class JTATransactionManagerTransactionFactory implements TransactionFactory {

	@Override
	public TransactionImplementor createTransaction(TransactionCoordinator coordinator) {
		return new JTATransactionManagerTransaction( coordinator );
	}

	@Override
	public boolean canBeDriver() {
		return true;
	}

	@Override
	public boolean compatibleWithJtaSynchronization() {
		return true;
	}

	@Override
	public boolean isJoinableJtaTransaction(TransactionCoordinator transactionCoordinator,
			TransactionImplementor transaction) {
		try {
			final JtaPlatform jtaPlatform = transactionCoordinator
					.getTransactionContext()
					.getTransactionEnvironment()
					.getJtaPlatform();
			if ( jtaPlatform == null ) {
				throw new TransactionException( "Unable to check transaction status" );
			}
			if ( jtaPlatform.retrieveTransactionManager() != null ) {
				return JtaStatusHelper.isActive( jtaPlatform.retrieveTransactionManager().getStatus() );
			}
			else {
				final UserTransaction ut = jtaPlatform.retrieveUserTransaction();
				return ut != null && JtaStatusHelper.isActive( ut );
			}
		}
		catch ( SystemException se ) {
			throw new TransactionException( "Unable to check transaction status", se );
		}
	}

	@Override
	public ConnectionReleaseMode getDefaultReleaseMode() {
		return ConnectionReleaseMode.AFTER_STATEMENT;
	}
}
