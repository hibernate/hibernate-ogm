/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.eventstate.impl;

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

	/**
	 * Initializes the {@link OperationCollector} at the beginning of an event cycle.
	 */
	static class OperationCollectorLifecycle implements EventStateLifecycle<OperationCollector> {

		static final EventStateLifecycle<?> INSTANCE = new OperationCollectorLifecycle();

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
	 * Initializes the {@link OperationsQueue} at the beginning of an event cycle and executes
	 * the operations it batches upon event finish.
	 *
	 * @author Gunnar Morling
	 */
	static class OperationsQueueLifecycle implements EventStateLifecycle<OperationsQueue> {

		static final EventStateLifecycle<?> INSTANCE = new OperationsQueueLifecycle();

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
			operationsQueue.close();
		}
	}
}
