/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.transaction.infinispan.impl;

import java.util.Properties;

import javax.transaction.SystemException;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.util.JTAHelper;

/**
 * TransactionFactory using JTA transactions exclusively from the TransactionManager
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class JTATransactionManagerTransactionFactory implements TransactionFactory {
	public Transaction createTransaction(JDBCContext jdbcContext, Context context)
			throws HibernateException {
		return new JTATransactionManagerTransaction(jdbcContext, context) ;
		//return new JBossTSTransaction(jdbcContext, context) ;
	}

	public void configure(Properties props) throws HibernateException {
	}

	public ConnectionReleaseMode getDefaultReleaseMode() {
		return ConnectionReleaseMode.ON_CLOSE;
	}

	public boolean isTransactionManagerRequired() {
		return true;
	}

	public boolean areCallbacksLocalToHibernateTransactions() {
		return false;
	}

	public boolean isTransactionInProgress(JDBCContext jdbcContext, Context transactionContext, Transaction transaction) {
		try {
			return JTAHelper.isTransactionInProgress(
					transactionContext.getFactory().getTransactionManager().getTransaction()
			);
		}
		catch( SystemException se ) {
			throw new TransactionException( "Unable to check transaction status", se );
		}
	}
}
