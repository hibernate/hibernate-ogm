/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.eventstate.impl;

import org.hibernate.HibernateException;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.ogm.util.impl.EffectivelyFinal;

/**
 * Delegating {@link FlushEventListener} which manages the {@link EventContextManager}.
 *
 * @author Gunnar Morling
 */
public class EventContextManagingFlushEventListener implements FlushEventListener {

	@EffectivelyFinal
	private FlushEventListener delegate;
	private final EventContextManager stateManager;

	public EventContextManagingFlushEventListener(EventContextManager stateManager) {
		this.stateManager = stateManager;
	}

	public void setDelegate(FlushEventListener delegate) {
		this.delegate = delegate;
	}

	@Override
	public void onFlush(FlushEvent event) throws HibernateException {
		stateManager.onEventBegin( event.getSession() );

		try {
			delegate.onFlush( event );
		}
		finally {
			stateManager.onEventFinished();
		}
	}

	public static class EventContextManagingFlushEventListenerDuplicationStrategy implements DuplicationStrategy {

		public static final DuplicationStrategy INSTANCE = new EventContextManagingFlushEventListenerDuplicationStrategy();

		private EventContextManagingFlushEventListenerDuplicationStrategy() {
		}

		@Override
		public boolean areMatch(Object listener, Object original) {
			if ( listener instanceof EventContextManagingFlushEventListener && original instanceof FlushEventListener ) {
				( (EventContextManagingFlushEventListener) listener ).setDelegate( (FlushEventListener) original );
				return true;
			}

			return false;
		}

		@Override
		public Action getAction() {
			return Action.REPLACE_ORIGINAL;
		}
	}
}
