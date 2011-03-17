package org.hibernate.ogm.datastore.infinispan.impl;

import java.util.Properties;

import javax.transaction.TransactionManager;

import org.hibernate.transaction.TransactionManagerLookupFactory;
import org.infinispan.transaction.lookup.TransactionManagerLookup;

/**
 * Wraps the TransactionManager lookup strategy as configured in the Hibernate main properties
 * into an implementation usable by Infinispan
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class TransactionManagerLookupDelegator implements TransactionManagerLookup {

	private final org.hibernate.transaction.TransactionManagerLookup transactionManagerLookup;
	private final Properties hibernateConfiguration;

	public TransactionManagerLookupDelegator(Properties hibernateConfiguration) {
		this.hibernateConfiguration = hibernateConfiguration;
		this.transactionManagerLookup = TransactionManagerLookupFactory.getTransactionManagerLookup( hibernateConfiguration );
	}

	@Override
	public TransactionManager getTransactionManager() throws Exception {
		if ( transactionManagerLookup != null ) {
			return transactionManagerLookup.getTransactionManager( hibernateConfiguration );
		}
		else {
			return null;
		}
	}

	protected boolean isValid() {
		return transactionManagerLookup != null;
	}

}
