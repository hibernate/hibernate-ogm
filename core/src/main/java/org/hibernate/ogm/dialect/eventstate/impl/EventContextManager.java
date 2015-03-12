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

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.ogm.util.impl.Immutable;
import org.hibernate.service.Service;

/**
 * A service which provides access to state specific to one event cycle (currently (auto)-flush or persist).
 * <p>
 * Client code (such as persisters, dialects etc.) may use this service to propagate state amongst each other, as long
 * as they are within the same event cycle. States are identified by class objects which are used as key when accessing
 * the contextual map. If a given state type is accessed for the first time during an event cycle, its associated
 * {@link EventStateProducer} will be invoked to obtain a new instance of that state type.
 * <p>
 * Accessing the context when not being within the scope of a supported event cycle is illegal.
 * <p>
 * The service state is managed by listeners such as {@link EventContextManagingAutoFlushEventListener} which make sure
 * that the context is destroyed upon event cycle completion.
 *
 * @author Gunnar Morling
 */
public class EventContextManager implements Service {

	private final ThreadLocal<Map<Class<?>, Object>> stateHolder;

	@Immutable
	private final Map<Class<?>, EventStateProducer<?>> producers;

	public EventContextManager(Map<Class<?>, EventStateProducer<?>> producers) {
		this.stateHolder = new ThreadLocal<>();
		this.producers = Collections.unmodifiableMap( producers );
	}

	void onEventBegin(EventSource session) {
		Map<Class<?>, Object> stateMap = new HashMap<>();
		stateMap.put( SessionImplementor.class, session );
		stateHolder.set( stateMap );
	}

	void onEventFinished() {
		stateHolder.remove();
	}

	/**
	 * Returns the state object of the given type.
	 * <p>
	 * <b>Note:</b> Must only be called when being within a supported event cycle.
	 */
	public <T> T get(Class<T> stateType) {
		@SuppressWarnings("unchecked")
		T value = (T) getStates().get( stateType );

		if ( value == null ) {
			value = create( stateType );
			getStates().put( stateType, value );
		}

		return value;
	}

	private <T> T create(Class<T> stateType) {
		@SuppressWarnings("unchecked")
		EventStateProducer<T> producer = (EventStateProducer<T>) producers.get( stateType );

		if ( producer == null ) {
			throw new IllegalStateException( "No producer found for state type: " + stateType );
		}

		SessionImplementor session = (SessionImplementor) stateHolder.get().get( SessionImplementor.class );

		return producer.produce( session );
	}

	private Map<Class<?>, Object> getStates() {
		Map<Class<?>, Object> states = stateHolder.get();

		if ( states == null ) {
			throw new IllegalStateException( "Must not access event cycle state if not within a supported event cycle " + "(flush, auto-flush, persist)" );
		}

		return states;
	}
}
