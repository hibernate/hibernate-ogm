/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import org.hibernate.ogm.dialect.spi.TransactionContext;

/**
 * Default implementation of the {@link TransactionContext}.
 *
 * @author Davide D'Alto
 */
public class TransactionContextImpl implements TransactionContext {

	private final Object transactionId;

	public TransactionContextImpl(IdentifiableDriver driver) {
		this.transactionId = driver.getTransactionId();
	}

	@Override
	public Object getTransactionId() {
		return transactionId;
	}
}
