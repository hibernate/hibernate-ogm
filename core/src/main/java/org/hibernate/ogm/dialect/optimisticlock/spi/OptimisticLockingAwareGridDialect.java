/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.optimisticlock.spi;

import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * A {@link GridDialect} facet to be implemented by those stores which support finding and altering versioned records in
 * an atomic fashion.
 *
 * @author Gunnar Morling
 */
public interface OptimisticLockingAwareGridDialect extends GridDialect {

	/**
	 * Updates the given tuple. Implementors are expected to perform the update only if the entity has the given old
	 * version in the datastore. Specifically, atomic "find and update" semantics should be applied.
	 *
	 * @param entityKey The key of the entity to save
	 * @param oldLockState A tuple with all those column values identifying the previous version of the record in the
	 * datastore. Implementations are expected to apply all the contained columns to select the record to be updated.
	 * @param tuple The entity column values to save
	 * @param tupleContext Provides additional meta-data useful for the tuple update
	 * @return {@code true} if the update succeeded, {@code false} otherwise (e.g. if the entity has been updated in the
	 * datastore in parallel, so it has a newer version then the given one).
	 */
	boolean updateTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, Tuple tuple, TupleContext tupleContext);

	/**
	 * Removes the given tuple. Implementors are expected to perform the delete only if the entity has the given old
	 * version in the datastore. Specifically, atomic "find and delete" semantics should be applied.
	 *
	 * @param entityKey The key of the entity to save
	 * @param oldLockState A tuple with all those column values identifying the previous version of the record in the
	 * datastore. Implementations are expected to apply all the contained columns to select the record to be removed.
	 * @param tupleContext Provides additional meta-data useful for the tuple removal
	 * @return {@code true} if the deletion succeeded, {@code false} otherwise (e.g. if the entity has been removed or
	 * updated in the datastore in parallel, so it has a newer version then the given one).
	 */
	boolean removeTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, TupleContext tupleContext);
}
