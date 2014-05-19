/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect;

import org.hibernate.ogm.dialect.batch.OperationsQueue;

/**
 * A {@link GridDialect} that can batch operations and execute them using the mechanism provided by the underlying database.
 * <p>
 * Which operations can be batched and when they are executed depends on the underlying database.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public interface BatchableGridDialect extends GridDialect {

	/**
	 * Executes the batched operations using the mechanism provided by the db
	 */
	void executeBatch(OperationsQueue queue);

}
