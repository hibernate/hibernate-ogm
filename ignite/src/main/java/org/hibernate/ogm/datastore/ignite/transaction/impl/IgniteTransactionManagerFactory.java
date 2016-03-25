/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.transaction.impl;

import javax.cache.configuration.Factory;
import javax.transaction.TransactionManager;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;

public class IgniteTransactionManagerFactory implements Factory<TransactionManager> {

	private static final long serialVersionUID = -4649196379875889970L;

	private final JtaPlatform platform;

	public IgniteTransactionManagerFactory(JtaPlatform platform) {
		this.platform = platform;
	}

	@Override
	public TransactionManager create() {
		TransactionManager transactionManager = null;
		ApplicationServer as = ApplicationServer.currentApplicationServer();
		if (as == ApplicationServer.WEBSPHERE) {
			transactionManager = DelegatingTransactionManager.transactionManager();
		} else {
			transactionManager = platform.retrieveTransactionManager();
		}
		return transactionManager;
	}
}
