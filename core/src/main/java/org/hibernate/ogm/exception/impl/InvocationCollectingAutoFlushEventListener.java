/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.impl;

import org.hibernate.HibernateException;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.ogm.transaction.impl.ErrorHandlerEnabledTransaction;
import org.hibernate.ogm.util.impl.EffectivelyFinal;

/**
 * Delegating {@link AutoFlushEventListener} which manages the {@link GridDialectInvocationCollector}.
 *
 * @author Gunnar Morling
 *
 */
public class InvocationCollectingAutoFlushEventListener implements AutoFlushEventListener {

	private final GridDialectInvocationCollector invocationCollector;
	@EffectivelyFinal private AutoFlushEventListener delegate;

	public InvocationCollectingAutoFlushEventListener(GridDialectInvocationCollector invocationCollector) {
		this.invocationCollector = invocationCollector;
	}

	public void setDelegate(AutoFlushEventListener delegate) {
		this.delegate = delegate;
	}

	@Override
	public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
		try {
			delegate.onAutoFlush( event );
		}
		finally {
			getErrorHandlerManager( event ).afterFlush( invocationCollector.getAppliedOperationsOfFlushCycle() );
			invocationCollector.finishFlushCycle();
		}
	}

	private ErrorHandlerManager getErrorHandlerManager(AutoFlushEvent event) {
		// TODO synch with Hardy re local TX
		ErrorHandlerEnabledTransaction transaction = (ErrorHandlerEnabledTransaction) event.getSession().getTransactionCoordinator().getTransaction();
		return transaction.getErrorHandlerManager();
	}

	public static class InvocationCollectingAutoFlushEventListenerDuplicationStrategy implements DuplicationStrategy {

		@Override
		public boolean areMatch(Object listener, Object original) {
			if ( listener instanceof InvocationCollectingAutoFlushEventListener && original instanceof AutoFlushEventListener ) {
				( (InvocationCollectingAutoFlushEventListener) listener ).setDelegate( (AutoFlushEventListener) original );
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
