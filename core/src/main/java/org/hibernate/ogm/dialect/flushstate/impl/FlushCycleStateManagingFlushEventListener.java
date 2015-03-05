/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.flushstate.impl;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.ogm.util.impl.EffectivelyFinal;

/**
 * Delegating {@link FlushEventListener} which manages the {@link FlushCycleStateManager}.
 *
 * @author Gunnar Morling
 */
public class FlushCycleStateManagingFlushEventListener implements FlushEventListener {

	@EffectivelyFinal
	private FlushEventListener delegate;

	public FlushCycleStateManagingFlushEventListener() {
	}

	public void setDelegate(FlushEventListener delegate) {
		this.delegate = delegate;
	}

	@Override
	public void onFlush(FlushEvent event) throws HibernateException {
		FlushCycleStateManager stateManager = ( (SessionFactoryImplementor) event.getSession().getSessionFactory() ).getServiceRegistry().getService(
				FlushCycleStateManager.class );

		stateManager.onFlushBegin( event.getSession() );

		try {
			delegate.onFlush( event );
		}
		finally {
			stateManager.onFlushFinished();
		}
	}

	public static class FlushCycleStateManagingFlushEventListenerDuplicationStrategy implements DuplicationStrategy {

		@Override
		public boolean areMatch(Object listener, Object original) {
			if ( listener instanceof FlushCycleStateManagingFlushEventListener && original instanceof FlushEventListener ) {
				( (FlushCycleStateManagingFlushEventListener) listener ).setDelegate( (FlushEventListener) original );
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
