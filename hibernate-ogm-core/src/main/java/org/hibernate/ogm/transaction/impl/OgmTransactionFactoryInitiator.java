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

import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.internal.TransactionFactoryInitiator;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.ogm.service.impl.OptionalServiceInitiator;
import org.hibernate.ogm.transaction.impl.JTATransactionManagerTransactionFactory;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Use JTATransactionManagerTransactionFactory as the default value if no TransactionFactory is set
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmTransactionFactoryInitiator extends OptionalServiceInitiator<TransactionFactory> {

	private static final Log log = LoggerFactory.make();

	public static final OgmTransactionFactoryInitiator INSTANCE = new OgmTransactionFactoryInitiator();

	@Override
	@SuppressWarnings( {"unchecked"})
	public Class<TransactionFactory> getServiceInitiated() {
		return TransactionFactory.class;
	}

	@Override
	protected TransactionFactory buildServiceInstance(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object strategy = configurationValues.get( Environment.TRANSACTION_STRATEGY );
		//Hibernate EntityManager sets to JdbcTransactionFactory when RESOURCE_LOCAL is used
		if ( strategy == null || JdbcTransactionFactory.class.getName().equals(strategy) ) {
			log.usingDefaultTransactionFactory();
			return new JTATransactionManagerTransactionFactory();
		}
		return TransactionFactoryInitiator.INSTANCE.initiateService(configurationValues, registry);
	}

	@Override
	protected BasicServiceInitiator<TransactionFactory> backupInitiator() {
		return TransactionFactoryInitiator.INSTANCE;
	}
}