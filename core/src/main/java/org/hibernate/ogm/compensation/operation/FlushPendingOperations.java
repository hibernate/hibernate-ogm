/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation.operation;

import java.util.List;

import org.hibernate.ogm.dialect.batch.spi.GroupingByEntityDialect;

/**
 * Represents one execution of
 * {@link GroupingByEntityDialect#flushPendingOperations(org.hibernate.ogm.model.key.spi.EntityKey, org.hibernate.ogm.dialect.spi.TupleContext)}.
 *
 * @author Guillaume Smet
 *
 */
public interface FlushPendingOperations extends GridDialectOperation {

	/**
	 * Returns the list of batched operations.
	 */
	List<GridDialectOperation> getOperations();
}
