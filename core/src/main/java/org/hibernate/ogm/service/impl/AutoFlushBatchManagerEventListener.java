/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.HibernateException;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.ogm.dialect.impl.BatchOperationsDelegator;

/**
 * Prepares and executes batched operations when an {@link AutoFlushEvent} is caught
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
public class AutoFlushBatchManagerEventListener extends BatchManagerEventListener<AutoFlushEventListener, AutoFlushEvent> implements AutoFlushEventListener {

	public AutoFlushBatchManagerEventListener(BatchOperationsDelegator gridDialect) {
		super( gridDialect );
	}

	@Override
	public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
		onEvent( event );
	}

	@Override
	void delegate(AutoFlushEventListener delegate, AutoFlushEvent event) {
		delegate.onAutoFlush( event );
	}

	/**
	 * Replace the original {@link AutoFlushEventListener} and use it as delegate
	 */
	public static class AutoFlushDuplicationStrategy implements DuplicationStrategy {

		@Override
		public boolean areMatch(Object listener, Object original) {
			boolean match = original instanceof AutoFlushEventListener && listener instanceof AutoFlushBatchManagerEventListener;
			if ( match ) {
				( (AutoFlushBatchManagerEventListener) listener ).setDelegate( (AutoFlushEventListener) original );
			}
			return match;
		}

		@Override
		public Action getAction() {
			return Action.REPLACE_ORIGINAL;
		}

	}

}
