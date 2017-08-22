/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.dialect.impl.EmptyTransactionContext;
import org.hibernate.ogm.dialect.impl.IdentifiableDriver;
import org.hibernate.ogm.dialect.impl.TransactionContextImpl;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinator.TransactionDriver;

/**
 * @author Davide D'Alto
 */
public final class TransactionContextHelper {

	private TransactionContextHelper() {
	}

	/**
	 * Return a transaction context given the session; it never returns {@code null}.
	 *
	 * @param session current {@link Session}
	 * @return the {@link TransactionContext}
	 */
	public static TransactionContext transactionContext(Session session) {
		return transactionContext( (SharedSessionContractImplementor) session );
	}

	/**
	 * Return a transaction context given the session implementor; it never returns {@code null}.
	 *
	 * @param session current {@link SessionImplementor}
	 * @return the {@link TransactionContext}
	 */
	public static TransactionContext transactionContext(SharedSessionContractImplementor session) {
		TransactionCoordinator transactionCoordinator = session.getTransactionCoordinator();
		if ( transactionCoordinator != null && transactionCoordinator.getTransactionDriverControl() != null ) {
			TransactionDriver driver = transactionCoordinator.getTransactionDriverControl();
			if ( driver instanceof IdentifiableDriver ) {
				return new TransactionContextImpl( (IdentifiableDriver) driver );
			}
		}
		return EmptyTransactionContext.INSTANCE;
	}
}
