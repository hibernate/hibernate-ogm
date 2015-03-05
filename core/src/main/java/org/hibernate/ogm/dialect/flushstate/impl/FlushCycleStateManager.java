/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.flushstate.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.service.Service;

/**
 * Provides access to state specific to one (auto-) flush cycle.
 *
 * @author Gunnar Morling
 */
public class FlushCycleStateManager implements Service {

	private final ThreadLocal<Map<Class<?>, Object>> flushCycleStateHolder;
	private final ThreadLocal<SessionImplementor> sessionHolder;
	private final Map<Class<?>, FlushCyleStateInitializer<?>> initializers;

	public FlushCycleStateManager(Map<Class<?>, FlushCyleStateInitializer<?>> initializers) {
		this.flushCycleStateHolder = new ThreadLocal<>();
		this.sessionHolder = new ThreadLocal<>();
		this.initializers = initializers;
	}

	void onFlushBegin(EventSource session) {
		flushCycleStateHolder.set( new HashMap<Class<?>, Object>() );
		sessionHolder.set( session );
	}

	void onFlushFinished() {
		flushCycleStateHolder.remove();
		sessionHolder.remove();
	}

	/**
	 * Returns the flush-cycle state of the given type.
	 * <p>
	 * <b>Note:</b> Must only be called during flushes.
	 */
	public <T> T get(Class<T> stateType) {
		@SuppressWarnings("unchecked")
		T value = (T) getStates().get( stateType );

		if ( value == null ) {
			value = initialize( stateType );
			getStates().put( stateType, value );
		}

		return value;
	}

	private <T> T initialize(Class<T> stateType) {
		@SuppressWarnings("unchecked")
		FlushCyleStateInitializer<T> initializer = (FlushCyleStateInitializer<T>) initializers.get( stateType );

		if ( initializer == null ) {
			throw new IllegalStateException( "No initializer found for state type: " + stateType );
		}

		return initializer.initialize( sessionHolder.get() );
	}

	private Map<Class<?>, Object> getStates() {
		Map<Class<?>, Object> states = flushCycleStateHolder.get();

		if ( states == null ) {
			throw new IllegalStateException( "Must not access flush cycle state if not within a flush" );
		}
		return states;
	}
}
