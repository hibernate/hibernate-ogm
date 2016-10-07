/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.batch.spi;

import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;

/**
 * A {@link GridDialect} that can group operations for a given entity.
 *
 * @author Guillaume Smet
 */
public interface GroupingByEntityDialect extends GridDialect {

	/**
	 * Execute all the changes collected in the {@link OperationsQueue}.
	 *
	 * @param operationsQueue the operations queue
	 */
	void executeBatch(OperationsQueue operationsQueue);

	/**
	 * Flush all the pending operations.
	 *
	 * @param entityKey the {@link EntityKey} of the entity which is the origin of this operation
	 * @param tupleContext the {@link TupleContext}
	 */
	void flushPendingOperations(EntityKey entityKey, TupleContext tupleContext);

}
