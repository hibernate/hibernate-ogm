/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.HibernateException;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.ogm.dialect.impl.BatchOperationsDelegator;

/**
 * Prepares and executes batched operations when a {@link FlushEvent} is caught
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
public class FlushBatchManagerEventListener extends BatchManagerEventListener<FlushEventListener, FlushEvent> implements FlushEventListener {

	public FlushBatchManagerEventListener(BatchOperationsDelegator gridDialect) {
		super( gridDialect );
	}

	@Override
	public void onFlush(FlushEvent event) throws HibernateException {
		onEvent( event );
	}

	@Override
	void delegate(FlushEventListener delegate, FlushEvent event) {
		delegate.onFlush( event );
	}

	/**
	 * Replace the original {@link FlushEventListener} with the {@link FlushBatchManagerEventListener} and use it as
	 * delegate
	 */
	public static class FlushDuplicationStrategy implements DuplicationStrategy {

		@Override
		public boolean areMatch(Object listener, Object original) {
			boolean match = original instanceof FlushEventListener && listener instanceof FlushBatchManagerEventListener;
			if ( match ) {
				( (FlushBatchManagerEventListener) listener ).setDelegate( (FlushEventListener) original );
			}
			return match;
		}

		@Override
		public Action getAction() {
			return Action.REPLACE_ORIGINAL;
		}

	}

}
