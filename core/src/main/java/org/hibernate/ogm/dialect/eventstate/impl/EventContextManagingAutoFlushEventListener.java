/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.eventstate.impl;

import org.hibernate.HibernateException;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.ogm.util.impl.EffectivelyFinal;

/**
 * Delegating {@link AutoFlushEventListener} which manages the {@link EventContextManager}.
 *
 * @author Gunnar Morling
 */
public class EventContextManagingAutoFlushEventListener implements AutoFlushEventListener {

	@EffectivelyFinal
	private AutoFlushEventListener delegate;
	private final EventContextManager stateManager;

	public EventContextManagingAutoFlushEventListener(EventContextManager stateManager) {
		this.stateManager = stateManager;
	}

	public void setDelegate(AutoFlushEventListener delegate) {
		this.delegate = delegate;
	}

	@Override
	public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
		stateManager.onEventBegin( event.getSession() );

		try {
			delegate.onAutoFlush( event );
		}
		finally {
			stateManager.onEventFinished();
		}
	}

	public static class EventContextManagingAutoFlushEventListenerDuplicationStrategy implements DuplicationStrategy {

		public static final DuplicationStrategy INSTANCE = new EventContextManagingAutoFlushEventListenerDuplicationStrategy();

		private EventContextManagingAutoFlushEventListenerDuplicationStrategy() {
		}

		@Override
		public boolean areMatch(Object listener, Object original) {
			if ( listener instanceof EventContextManagingAutoFlushEventListener && original instanceof AutoFlushEventListener ) {
				( (EventContextManagingAutoFlushEventListener) listener ).setDelegate( (AutoFlushEventListener) original );
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
