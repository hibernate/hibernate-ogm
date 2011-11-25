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
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.transaction.TransactionManagerLookup;

/**
 * Encapsulate Infinispan's DummyTransactionManager
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DummyTransactionManagerLookup implements TransactionManagerLookup {
	private final org.infinispan.transaction.lookup.DummyTransactionManagerLookup infinispanLookup;

	public DummyTransactionManagerLookup() {
		this.infinispanLookup = new org.infinispan.transaction.lookup.DummyTransactionManagerLookup();
	}
	public TransactionManager getTransactionManager(Properties props) throws HibernateException {
		try {
			return infinispanLookup.getTransactionManager();
		}
		catch ( Exception e ) {
			throw new HibernateException( "Unable to get DummyTransactionManager", e );
		}
	}

	public String getUserTransactionName() {
		return null;
	}

	public Object getTransactionIdentifier(Transaction transaction) {
		return transaction;
	}
}
