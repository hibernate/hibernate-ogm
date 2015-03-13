/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.eventstate.impl;

import java.util.Collections;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.failure.impl.OperationCollector;
import org.hibernate.ogm.transaction.impl.ErrorHandlerEnabledTransactionDecorator;

/**
 * Holds all known {@link EventStateProducer}s.
 *
 * @author Gunnar Morling
 */
public class EventStateProducers {

	private EventStateProducers() {
	}

	public static Map<Class<?>, EventStateProducer<?>> getProducers(Map<?, ?> configuration) {
		if ( configuration.containsKey( OgmProperties.ERROR_HANDLER ) ) {
			return Collections.<Class<?>, EventStateProducer<?>>singletonMap( OperationCollector.class, OperationCollectorProducer.INSTANCE );
		}
		else {
			return Collections.emptyMap();
		}
	}

	/**
	 * Initializes the {@link OperationCollector} if accessed for the first time during a given event cycle.
	 */
	private static class OperationCollectorProducer implements EventStateProducer<OperationCollector> {

		private static EventStateProducer<?> INSTANCE = new OperationCollectorProducer();

		@Override
		public OperationCollector produce(SessionImplementor session) {
			return ( (ErrorHandlerEnabledTransactionDecorator) session.getTransactionCoordinator().getTransaction() ).getOperationCollector();
		}
	}
}
