/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.impl;

import javax.transaction.TransactionManager;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.infinispan.transaction.lookup.TransactionManagerLookup;

/**
 * Wraps the TransactionManager lookup strategy as configured in the Hibernate main properties
 * into an implementation usable by Infinispan
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
public class TransactionManagerLookupDelegator implements TransactionManagerLookup {

	private final JtaPlatform platform;

	public TransactionManagerLookupDelegator(JtaPlatform platform) {
		this.platform = platform;
	}

	@Override
	public TransactionManager getTransactionManager() throws Exception {
		if ( platform != null ) {
			return platform.retrieveTransactionManager();
		}
		else {
			return null;
		}
	}

	protected boolean isValid() {
		return platform != null ? platform.retrieveTransactionManager() != null : false;
	}

}
