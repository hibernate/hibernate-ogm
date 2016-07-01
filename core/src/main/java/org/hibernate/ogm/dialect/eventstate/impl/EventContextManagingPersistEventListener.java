/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.eventstate.impl;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import org.hibernate.ogm.util.impl.EffectivelyFinal;

/**
 * Delegating {@link PersistEventListener} which manages the {@link EventContextManager}.
 *
 * @author Gunnar Morling
 */
public class EventContextManagingPersistEventListener implements PersistEventListener {

	@EffectivelyFinal
	private PersistEventListener delegate;
	private final EventContextManager stateManager;

	public EventContextManagingPersistEventListener(EventContextManager stateManager) {
		this.stateManager = stateManager;
	}

	public void setDelegate(PersistEventListener delegate) {
		this.delegate = delegate;
	}

	@Override
	public void onPersist(PersistEvent event) throws HibernateException {
		stateManager.onEventBegin( event.getSession() );

		try {
			delegate.onPersist( event );
		}
		finally {
			stateManager.onEventFinished();
		}
	}

	@Override
	public void onPersist(PersistEvent event, Map createdAlready) throws HibernateException {
		stateManager.onEventBegin( event.getSession() );

		try {
			delegate.onPersist( event, createdAlready );
		}
		finally {
			stateManager.onEventFinished();
		}
	}

	public static class EventContextManagingPersistEventListenerDuplicationStrategy implements DuplicationStrategy {

		public static final DuplicationStrategy INSTANCE = new EventContextManagingPersistEventListenerDuplicationStrategy();

		private EventContextManagingPersistEventListenerDuplicationStrategy() {
		}

		@Override
		public boolean areMatch(Object listener, Object original) {
			if ( listener instanceof EventContextManagingPersistEventListener && original instanceof PersistEventListener ) {
				( (EventContextManagingPersistEventListener) listener ).setDelegate( (PersistEventListener) original );
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
