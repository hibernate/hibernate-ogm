/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.compensation.operation;

import org.hibernate.ogm.dialect.optimisticlock.spi.OptimisticLockingAwareGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;

/**
 * Represents the execution of one specific method of the {@link GridDialect} contract or its optional facets such as
 * {@link OptimisticLockingAwareGridDialect}. Sub-types expose the parameters of the invocation and other context as
 * needed.
 *
 * @author Gunnar Morling
 */
public interface GridDialectOperation {

	/**
	 * Returns the specific type this operation.
	 */
	OperationType getType();

	/**
	 * Narrows this operation down to the given sub-type. The type should be checked before via {@link #getType()}:
	 */
	<T extends GridDialectOperation> T as(Class<T> type);
}
