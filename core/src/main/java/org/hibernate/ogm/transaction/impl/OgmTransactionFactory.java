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
 * {@code TransactionFactory} allowing to choose between JTA transactions retrieved from the {@code TransactionManager}
 * or to emulate transactions via {@code EmulatedLocalTransaction}.
 *
 * The latter is not a proper {@code Transaction} implementation, but only used to make sure that the appropriate
 * flush events are triggered at commit time. This transaction can be useful in the case where the configured transaction
 * type is resource local and the data store does not implement transactions (eg MongoDB).
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmTransactionFactory implements TransactionFactory {

	private final boolean emulateTransaction;

	public OgmTransactionFactory(boolean emulateTransaction) {
		this.emulateTransaction = emulateTransaction;
	}

	@Override
	public TransactionImplementor createTransaction(TransactionCoordinator coordinator) {
		if ( emulateTransaction ) {
			return new EmulatedLocalTransaction( coordinator );
		}
		else {
			return new JTATransaction( coordinator );
		}
	}

	@Override
	public boolean canBeDriver() {
		return true;
	}

	@Override
	public boolean compatibleWithJtaSynchronization() {
		return !emulateTransaction;
	}

	@Override
	public boolean isJoinableJtaTransaction(TransactionCoordinator transactionCoordinator,
			TransactionImplementor transaction) {
		if ( emulateTransaction ) {
			return false;
		}

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
