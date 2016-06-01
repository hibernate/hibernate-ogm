/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.eventstate.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.compensation.impl.ErrorHandlerEnabledTransactionCoordinatorDecorator;
import org.hibernate.ogm.compensation.impl.OperationCollector;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.impl.BatchOperationsDelegator;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.GridDialect;

/**
 * Holds all known {@link EventStateLifecycle}s.
 *
 * @author Gunnar Morling
 */
class EventStateLifecycles {

	private EventStateLifecycles() {
	}

	public static Map<Class<?>, EventStateLifecycle<?>> getLifecycles() {
		Map<Class<?>, EventStateLifecycle<?>> lifecycles = new HashMap<>();

		lifecycles.put( OperationCollector.class, OperationCollectorLifecycle.INSTANCE );
		lifecycles.put( OperationsQueue.class, OperationsQueueLifecycle.INSTANCE );

		return lifecycles;
	}

	/**
	 * Initializes the {@link OperationCollector} if accessed for the first time during a given event cycle.
	 */
	private static class OperationCollectorLifecycle implements EventStateLifecycle<OperationCollector> {

		private static EventStateLifecycle<?> INSTANCE = new OperationCollectorLifecycle();

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
		public OperationsQueue create(SessionImplementor session) {
			return new OperationsQueue();
		}

		@Override
		public void onFinish(OperationsQueue operationsQueue, SessionImplementor session) {
			GridDialect gridDialect = session.getFactory()
					.getServiceRegistry()
					.getService( GridDialect.class );

			GridDialects.getDelegateOrNull( gridDialect, BatchOperationsDelegator.class ).executeBatch( operationsQueue );
		}
	}
}
