/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.dialect.impl.IdentifiableDriver;
import org.hibernate.ogm.dialect.impl.TransactionContextImpl;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.TransactionCoordinator.TransactionDriver;

/**
 * @author Davide D'Alto
 */
public final class TransactionContextHelper {

	private TransactionContextHelper() {
	}

	public static TransactionContext transactionContext(Session session) {
		return transactionContext( (SessionImplementor) session );
	}

	public static TransactionContext transactionContext(SessionImplementor session) {
		TransactionCoordinator transactionCoordinator = session.getTransactionCoordinator();
		if ( transactionCoordinator != null && transactionCoordinator.getTransactionDriverControl() != null ) {
			TransactionDriver driver = transactionCoordinator.getTransactionDriverControl();
			if ( driver instanceof IdentifiableDriver ) {
				return new TransactionContextImpl( (IdentifiableDriver) driver );
			}
		}
		return null;
	}
}
