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
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.ogm.util.impl.EffectivelyFinal;

/**
 * Delegating {@link AutoFlushEventListener} which manages the {@link FlushCycleStateManager}.
 *
 * @author Gunnar Morling
 */
public class FlushCycleStateManagingAutoFlushEventListener implements AutoFlushEventListener {

	@EffectivelyFinal
	private AutoFlushEventListener delegate;

	public FlushCycleStateManagingAutoFlushEventListener() {
	}

	public void setDelegate(AutoFlushEventListener delegate) {
		this.delegate = delegate;
	}

	@Override
	public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
		FlushCycleStateManager stateManager = ( (SessionFactoryImplementor) event.getSession().getSessionFactory() ).getServiceRegistry().getService(
				FlushCycleStateManager.class );

		stateManager.onFlushBegin( event.getSession() );

		try {
			delegate.onAutoFlush( event );
		}
		finally {
			stateManager.onFlushFinished();
		}
	}

	public static class FlushCycleStateManagingAutoFlushEventListenerDuplicationStrategy implements DuplicationStrategy {

		@Override
		public boolean areMatch(Object listener, Object original) {
			if ( listener instanceof FlushCycleStateManagingAutoFlushEventListener && original instanceof AutoFlushEventListener ) {
				( (FlushCycleStateManagingAutoFlushEventListener) listener ).setDelegate( (AutoFlushEventListener) original );
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
