/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.ogm.id.spi.NextValueRequest;
import org.hibernate.ogm.massindex.spi.Consumer;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationKey;
import org.hibernate.ogm.model.spi.EntityKey;
import org.hibernate.ogm.model.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.RowKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.util.Experimental;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.service.Service;
import org.hibernate.type.Type;

/**
 * Dialect abstracting Hibernate OGM from the grid implementation
 * <p>
 * Rather than implementing this interface directly, it is recommend to extend {@link BaseGridDialect}.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public interface GridDialect extends Service {

	LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode);

	/**
	 * Return the tuple with the given column for a given key
	 */
	Tuple getTuple(EntityKey key, TupleContext tupleContext);

	/**
	 * Return a new tuple for a given key
	 * Only used if the tuple is not present
	 */
	Tuple createTuple(EntityKey key, TupleContext tupleContext);

	/**
	 * Update the tuple for a given key or null if not present
	 */
	void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext);

	/**
	 * Remove the tuple for a given key
	 */
	void removeTuple(EntityKey key, TupleContext tupleContext);

	/**
	 * Return the list of tuples corresponding to a given association and the given context
	 */
	Association getAssociation(AssociationKey key, AssociationContext associationContext);

	/**
	 * Create an empty container for the list of tuples corresponding to a given association
	 * Only used if the association data is not present
	 */
	Association createAssociation(AssociationKey key, AssociationContext associationContext);

	/**
	 * Update a given list of tuples corresponding to a given association
	 */
	void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext);

	/**
	 * Remove the list of tuples corresponding to a given association
	 */
	void removeAssociation(AssociationKey key, AssociationContext associationContext);

	Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey);

	/**
	 * Whether the given association is stored within an entity structure or not. E.g. dialects for document stores may
	 * support storing associations within entity documents and would have to return {@code true} if this is the case
	 * for a given association.
	 *
	 * @param associationKey identifies the association of interest
	 * @return {@code true} if the specified association is stored within an entity structure, {@code false} otherwise.
	 */
	boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext);

	/**
	 * Returns the next value from the specified id generator with the specified increment.
	 *
	 * @return the next value from the specified id generator
	 */
	Number nextValue(NextValueRequest request);

	/**
	 * Whether this dialect supports sequences for id generation or not. If not, a table-based strategy is expected to
	 * be used as fall-back.
	 *
	 * @return {@code true} if this dialect supports sequences, {@code false} otherwise.
	 */
	boolean supportsSequences();

	/**
	 * Let the dialect override types if required to customize them to the datastore. Returns the GridType instance to
	 * use to bind the given {@code type} or null if not overridden.
	 * <p>
	 * Most types should not be overridden and thus return null
	 */
	@Experimental( "Custom types including the GridType contract will be re-visited after OGM 4.1.0.Final." )
	GridType overrideType(Type type);

	/**
	 * A consumer is called for each tuple matching the selected {@link EntityKeyMetadata}.
	 *
	 * @param consumer
	 *            the instance that is going to be called for every {@link Tuple}
	 * @param entityKeyMetadatas
	 *            the key metadata of the tables for which we want to apply the costumer
	 */
	void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas);
}
