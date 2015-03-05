/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.flushstate.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.exception.impl.ErrorHandlerManager;
import org.hibernate.ogm.transaction.impl.ErrorHandlerEnabledTransaction;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contributes the {@link FlushCycleStateManager} service if needed.
 *
 * @author Gunnar Morling
 */
@SuppressWarnings("rawtypes")
public class FlushCycleStateManagerInitiator implements StandardServiceInitiator<FlushCycleStateManager> {

	public static final FlushCycleStateManagerInitiator INSTANCE = new FlushCycleStateManagerInitiator();

	private FlushCycleStateManagerInitiator() {
	}

	@Override
	public Class<FlushCycleStateManager> getServiceInitiated() {
		return FlushCycleStateManager.class;
	}

	@Override
	public FlushCycleStateManager initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		Map<Class<?>, FlushCyleStateInitializer<?>> initializers = new HashMap<>();

		if ( configurationValues.containsKey( OgmProperties.ERROR_HANDLER ) ) {
			initializers.put( ErrorHandlerManager.class, new ErrorHandlerManagerInitializer() );
		}

		if ( !initializers.isEmpty() ) {
			return new FlushCycleStateManager( initializers );
		}

		return null;
	}

	private static class ErrorHandlerManagerInitializer implements FlushCyleStateInitializer<ErrorHandlerManager> {

		@Override
		public ErrorHandlerManager initialize(SessionImplementor session) {
			return ( (ErrorHandlerEnabledTransaction) session.getTransactionCoordinator().getTransaction() ).getErrorHandlerManager();
		}
	}
}
