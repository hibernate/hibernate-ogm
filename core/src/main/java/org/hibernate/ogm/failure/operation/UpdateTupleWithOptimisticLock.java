/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.failure.operation;

import org.hibernate.ogm.dialect.optimisticlock.spi.OptimisticLockingAwareGridDialect;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Represents one execution of
 * {@link OptimisticLockingAwareGridDialect#updateTupleWithOptimisticLock(EntityKey, Tuple, Tuple, org.hibernate.ogm.dialect.spi.TupleContext)}
 *
 * @author Gunnar Morling
 */
public interface UpdateTupleWithOptimisticLock extends GridDialectOperation {

	EntityKey getEntityKey();

	Tuple getOldLockState();

	Tuple getTuple();
}
