/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import org.hibernate.ogm.dialect.spi.TransactionContext;

/**
 * @author Davide D'Alto
 */
public class TransactionContextImpl implements TransactionContext {

	private final IdentifiableDriver driver;

	public TransactionContextImpl(IdentifiableDriver driver) {
		this.driver = driver;
	}

	@Override
	public Long getTransactionId() {
		return driver.getTransactionId();
	}
}
