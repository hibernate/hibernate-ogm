/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.eventstate.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.compensation.impl.ErrorHandlerEnabledTransactionCoordinatorDecorator;
import org.hibernate.ogm.compensation.impl.OperationCollector;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.impl.BatchOperationsDelegator;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.util.impl.Immutable;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Holds all known {@link EventStateLifecycle}s.
 *
 * @author Gunnar Morling
 */
class EventStateLifecycles {

	public static final EventStateLifecycles INSTANCE = new EventStateLifecycles();

	@Immutable
	private final Map<Class<?>, EventStateLifecycle<?>> lifecycles;

	private EventStateLifecycles() {
		Map<Class<?>, EventStateLifecycle<?>> lifecycles = new HashMap<>();

		lifecycles.put( OperationCollector.class, OperationCollectorLifecycle.INSTANCE );
		lifecycles.put( OperationsQueue.class, OperationsQueueLifecycle.INSTANCE );

		this.lifecycles = Collections.unmodifiableMap( lifecycles );
	}

	public Map<Class<?>, EventStateLifecycle<?>> getEnabledLifecycles(ServiceRegistryImplementor serviceRegistry) {
		Map<Class<?>, EventStateLifecycle<?>> enabledLifecycles = new HashMap<>();

		for ( Entry<Class<?>, EventStateLifecycle<?>> lifecycle : lifecycles.entrySet() ) {
			if ( lifecycle.getValue().mustBeEnabled( serviceRegistry ) ) {
				enabledLifecycles.put( lifecycle.getKey(), lifecycle.getValue() );
			}
		}

		return enabledLifecycles;
	}

	/**
	 * Initializes the {@link OperationCollector} if accessed for the first time during a given event cycle.
	 */
	private static class OperationCollectorLifecycle implements EventStateLifecycle<OperationCollector> {

		private static EventStateLifecycle<?> INSTANCE = new OperationCollectorLifecycle();

		@Override
		public boolean mustBeEnabled(ServiceRegistryImplementor serviceRegistry) {
			return serviceRegistry.getService( ConfigurationService.class ).getSettings().containsKey( OgmProperties.ERROR_HANDLER );
		}

		@Override
		public OperationCollector create(SessionImplementor session) {
			return ( (ErrorHandlerEnabledTransactionCoordinatorDecorator) session.getTransactionCoordinator() ).getOperationCollector();
		}

		@Override
		public void onFinish(OperationCollector state, SessionImplementor session) {
			// nothing to do
		}
	}

	/**
	 * Initializes the {@link OperationsQueue} if accessed for the first time during a given event cycle and executes
	 * the operations it batches upon event finish.
	 *
	 * @author Gunnar Morling
	 */
	private static class OperationsQueueLifecycle implements EventStateLifecycle<OperationsQueue> {

		private static EventStateLifecycle<?> INSTANCE = new OperationsQueueLifecycle();

		@Override
		public boolean mustBeEnabled(ServiceRegistryImplementor serviceRegistry) {
			GridDialect gridDialect = serviceRegistry.getService( GridDialect.class );
			BatchOperationsDelegator batchDelegator = GridDialects.getDelegateOrNull( gridDialect, BatchOperationsDelegator.class );
			return batchDelegator != null;
		}

		@Override
		public OperationsQueue create(SessionImplementor session) {
			return new OperationsQueue();
		}

		@Override
		public void onFinish(OperationsQueue operationsQueue, SessionImplementor session) {
			GridDialect gridDialect = session.getFactory()
					.getServiceRegistry()
					.getService( GridDialect.class );

			if ( operationsQueue.size() > 0 ) {
				GridDialects.getDelegateOrNull( gridDialect, BatchOperationsDelegator.class ).executeBatch( operationsQueue );
			}

			operationsQueue.close();
		}
	}
}
