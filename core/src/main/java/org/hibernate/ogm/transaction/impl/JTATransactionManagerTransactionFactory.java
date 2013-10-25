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
 * TransactionFactory using JTA transactions exclusively from the TransactionManager
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class JTATransactionManagerTransactionFactory implements TransactionFactory {

	@Override
	public TransactionImplementor createTransaction(TransactionCoordinator coordinator) {
		return new JTATransactionManagerTransaction( coordinator );
	}

	@Override
	public boolean canBeDriver() {
		return true;
	}

	@Override
	public boolean compatibleWithJtaSynchronization() {
		return true;
	}

	@Override
	public boolean isJoinableJtaTransaction(TransactionCoordinator transactionCoordinator,
			TransactionImplementor transaction) {
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
