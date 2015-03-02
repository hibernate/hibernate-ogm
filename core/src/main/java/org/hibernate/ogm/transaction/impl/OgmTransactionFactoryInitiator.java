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
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Use {@code OgmTransactionFactory} as the default value if no {@code TransactionFactory} is set.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@SuppressWarnings("rawtypes")
public class OgmTransactionFactoryInitiator implements StandardServiceInitiator<TransactionFactory> {

	public static final OgmTransactionFactoryInitiator INSTANCE = new OgmTransactionFactoryInitiator();

	private static final Log log = LoggerFactory.make();

	@Override
	public Class<TransactionFactory> getServiceInitiated() {
		return TransactionFactory.class;
	}

	@Override
	public TransactionFactory<?> initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		// if there is a explicitly set transaction factory let ORM instantiate it
		if ( hasExplicitNonJPAResourceLocalTransactionFactory( configurationValues ) ) {
			return TransactionFactoryInitiator.INSTANCE.initiateService( configurationValues, registry );
		}

		// if the strategy is not explicitly set or resource local we decide based on the dialect
		DatastoreProvider datastoreProvider = registry.getService( DatastoreProvider.class );
		boolean emulateTransactions;
		if ( datastoreProvider.allowsTransactionEmulation() ) {
			// for resource local transaction type where the datastore does not support transactions
			// it is enough to simulate transaction. In this case transactions are just used to scope a unit
			// of work and to make sure that the appropriate flush event occurs
			emulateTransactions = true;
		}
		else {
			log.usingDefaultTransactionFactory();
			emulateTransactions = false;
		}
		return new OgmTransactionFactory( emulateTransactions );
	}

	private boolean hasExplicitNonJPAResourceLocalTransactionFactory(Map configurationValues) {
		final Object strategy = configurationValues.get( Environment.TRANSACTION_STRATEGY );
		return strategy != null && !isResourceLocalTransactionType( strategy );
	}

	private boolean isResourceLocalTransactionType(Object strategy) {
		return JdbcTransactionFactory.class.getName().equals( strategy ) || JdbcTransactionFactory.class.equals(
				strategy
		);
	}
}
