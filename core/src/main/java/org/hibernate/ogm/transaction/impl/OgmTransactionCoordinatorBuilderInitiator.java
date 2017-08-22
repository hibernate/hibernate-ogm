/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.compensation.ErrorHandler;
import org.hibernate.ogm.compensation.impl.ErrorHandlerEnabledTransactionCoordinatorBuilder;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.transaction.emulated.impl.EmulatedLocalTransactionCoordinatorBuilder;
import org.hibernate.ogm.transaction.jta.impl.RollbackOnCommitFailureJtaTransactionCoordinatorBuilder;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.resource.transaction.internal.TransactionCoordinatorBuilderInitiator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes OGM's {@link TransactionCoordinatorBuilder}.
 * <p>
 * Makes use of ORM's default builder in the case of JTA, otherwise {@link EmulatedLocalTransactionCoordinatorBuilder}
 * is used. In case an {@link ErrorHandler} has been configured, the actual builder will be wrapped by
 * {@link ErrorHandlerEnabledTransactionCoordinatorBuilder}.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class OgmTransactionCoordinatorBuilderInitiator implements StandardServiceInitiator<TransactionCoordinatorBuilder> {

	public static final OgmTransactionCoordinatorBuilderInitiator INSTANCE = new OgmTransactionCoordinatorBuilderInitiator();

	private OgmTransactionCoordinatorBuilderInitiator() {
	}

	@Override
	public Class<TransactionCoordinatorBuilder> getServiceInitiated() {
		return TransactionCoordinatorBuilder.class;
	}

	@Override
	public TransactionCoordinatorBuilder initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		TransactionCoordinatorBuilder builder = null;

		DatastoreProvider datastoreProvider = registry.getService( DatastoreProvider.class );
		TransactionCoordinatorBuilder defaultBuilder = TransactionCoordinatorBuilderInitiator.INSTANCE.initiateService( configurationValues, registry );
		TransactionCoordinatorBuilder customBuilder = datastoreProvider.getTransactionCoordinatorBuilder( defaultBuilder );

		// Take custom builder if present
		if ( customBuilder != null && customBuilder != defaultBuilder ) {
			builder = customBuilder;
		}
		// JTA default builder can be taken as is
		else if ( defaultBuilder.isJta() ) {
			builder = defaultBuilder;
		}
		// For RESOURCE_LOCAL either use emulated local TX or delegate to JTA
		else if ( datastoreProvider.allowsTransactionEmulation() ) {
			// if the datastore does not support transactions it is enough to emulate them. In this case transactions
			// are just used to scope a unit of work and to make sure that the appropriate flush event occurs
			builder = new EmulatedLocalTransactionCoordinatorBuilder( defaultBuilder );
		}
		else {
			builder = new RollbackOnCommitFailureJtaTransactionCoordinatorBuilder( getDefaultBuilder( registry, "jta" ) );
		}

		return decorateWithErrorHandlerIfNeeded( configurationValues, registry, builder );
	}

	private TransactionCoordinatorBuilder getDefaultBuilder(ServiceRegistryImplementor registry, String strategy ) {
		return registry.getService( StrategySelector.class ).resolveStrategy( TransactionCoordinatorBuilder.class, strategy );
	}

	private TransactionCoordinatorBuilder decorateWithErrorHandlerIfNeeded(Map configurationValues, ServiceRegistryImplementor registry, TransactionCoordinatorBuilder builder) {
		ErrorHandler errorHandler = getErrorHandler( configurationValues, registry );
		return errorHandler != null ? new ErrorHandlerEnabledTransactionCoordinatorBuilder( builder, errorHandler ) : builder;
	}

	private ErrorHandler getErrorHandler(Map<?, ?> configurationValues, ServiceRegistryImplementor registry) {
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationValues, registry.getService( ClassLoaderService.class ) );

		return propertyReader.property( OgmProperties.ERROR_HANDLER, ErrorHandler.class ).instantiate().getValue();
	}
}
