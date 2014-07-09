/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.internal.TransactionFactoryInitiator;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.ogm.service.impl.OptionalServiceInitiator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Use JTATransactionManagerTransactionFactory as the default value if no TransactionFactory is set
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@SuppressWarnings("rawtypes")
public class OgmTransactionFactoryInitiator extends OptionalServiceInitiator<TransactionFactory> {

	public static final OgmTransactionFactoryInitiator INSTANCE = new OgmTransactionFactoryInitiator();

	private static final Log log = LoggerFactory.make();

	@Override
	public Class<TransactionFactory> getServiceInitiated() {
		return TransactionFactory.class;
	}

	@Override
	protected TransactionFactory<?> buildServiceInstance(Map configurationValues, ServiceRegistryImplementor registry) {
		final Object strategy = configurationValues.get( Environment.TRANSACTION_STRATEGY );

		// Hibernate EntityManager sets to JdbcTransactionFactory when RESOURCE_LOCAL is used
		if ( strategy == null || representsJdbcTransactionFactory( strategy ) ) {
			log.usingDefaultTransactionFactory();
			return new JTATransactionManagerTransactionFactory();
		}

		return TransactionFactoryInitiator.INSTANCE.initiateService( configurationValues, registry );
	}

	private boolean representsJdbcTransactionFactory(final Object strategy) {
		return JdbcTransactionFactory.class.getName().equals( strategy ) || JdbcTransactionFactory.class.equals( strategy );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected StandardServiceInitiator<TransactionFactory> backupInitiator() {
		return TransactionFactoryInitiator.INSTANCE;
	}
}
