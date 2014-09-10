/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.impl.BatchOperationsDelegator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Contains the methods to signal to a {@link BatchableGridDialect} when to prepare for the execution of batch
 * operations and when to execute them
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @param <L> the type of the delegate
 */
abstract class BatchManagerEventListener<L, E extends AbstractEvent> {

	private static final Log log = LoggerFactory.make();

	private final BatchOperationsDelegator gridDialect;

	private L delegate;

	/**
	 * @param gridDialect the dialect that can execute batch operations
	 */
	public BatchManagerEventListener(BatchOperationsDelegator gridDialect) {
		this.gridDialect = gridDialect;
	}

	void onEvent(E event) {
		try {
			log.tracef( "%s %s", event.getClass(), " - begin" );
			gridDialect.prepareBatch();
			delegate( delegate, event );
			gridDialect.executeBatch();
			log.tracef( "%s %s", event.getClass(), " - end" );
		}
		finally {
			gridDialect.clearBatch();
		}
	}

	abstract void delegate(L delegate, E event);

	public void setDelegate(L delegate) {
		this.delegate = delegate;
	}

}
