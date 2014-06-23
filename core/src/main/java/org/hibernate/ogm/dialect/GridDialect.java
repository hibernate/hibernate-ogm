/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.IdGeneratorKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.util.ClosableIterator;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.service.Service;
import org.hibernate.type.Type;

/**
 * Dialect abstracting Hibernate OGM from the grid implementation
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
	 * Update value with the guaranteed next value with the defined increment
	 *
	 * Especially experimental
	 */
	void nextValue(IdGeneratorKey key, IntegralDataTypeHolder value, int increment, int initialValue);

	/**
	 * Whether this dialect supports sequences for id generation or not. If not, a table-based strategy is expected to
	 * be used as fall-back.
	 *
	 * @return {@code true} if this dialect supports sequences, {@code false} otherwise.
	 */
	boolean supportsSequences();

	/**
	 * Let the dialect override types if required to customize them to the datastore.
	 * Returns the GridType instance to use to bind the given {@code type} or null if not overridden.
	 *
	 * Most types should not be overridden and thus return null
	 *
	 * Experimental: this API might change in the future
	 */
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

	/**
	 * Returns the result of a native query executed on the backend.
	 *
	 * @param customQuery the query to execute on the backend
	 * @param queryParameters parameters passed for this query
	 * @return an {@link ClosableIterator} with the result of the query
	 */
	ClosableIterator<Tuple> executeBackendQuery(BackendCustomQuery customQuery, QueryParameters queryParameters);

	/**
	 * Returns a builder for retrieving parameter meta-data from native queries in this datastore's format.
	 *
	 * @return a builder for retrieving parameter meta-data
	 */
	ParameterMetadataBuilder getParameterMetadataBuilder();
}
