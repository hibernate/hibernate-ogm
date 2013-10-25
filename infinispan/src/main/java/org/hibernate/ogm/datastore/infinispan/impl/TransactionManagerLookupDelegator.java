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
package org.hibernate.ogm.datastore.infinispan.impl;

import javax.transaction.TransactionManager;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.infinispan.transaction.lookup.TransactionManagerLookup;

/**
 * Wraps the TransactionManager lookup strategy as configured in the Hibernate main properties
 * into an implementation usable by Infinispan
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
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
