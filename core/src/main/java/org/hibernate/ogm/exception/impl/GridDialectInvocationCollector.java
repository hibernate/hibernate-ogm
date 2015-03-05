/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.exception.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.exception.operation.spi.GridDialectOperation;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.Service;

/**
 * Collects all {@link GridDialect} operations performed within specific flush cycles.
 *
 * @author Gunnar Morling
 */
public class GridDialectInvocationCollector implements Service {

	private static final Log LOG = LoggerFactory.make();

	/**
	 * The operations successfully applied during the current flush cycle.
	 * <p>
	 * As services are concurrently used from several threads working with multiple sessions from one session factory,
	 * this is kept within a {@link ThreadLocal} which is reset upon finishing a flush cycle.
	 */
	private final ThreadLocal<List<GridDialectOperation>> appliedOperations = new ThreadLocal<List<GridDialectOperation>>() {

		@Override
		protected List<GridDialectOperation> initialValue() {
			return new ArrayList<GridDialectOperation>();
		};
	};

	public void add(GridDialectOperation operation) {
		LOG.debugf( "Adding grid operation %s", operation );
		appliedOperations.get().add( operation );
	}

	public List<GridDialectOperation> getAppliedOperationsOfFlushCycle() {
		return appliedOperations.get();
	}

	public void finishFlushCycle() {
		LOG.debug( "Finishing flush cyle" );
		appliedOperations.remove();
	}
}
