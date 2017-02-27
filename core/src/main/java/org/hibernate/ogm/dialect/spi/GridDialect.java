/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.spi;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.util.Experimental;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.service.Service;
import org.hibernate.type.Type;

/**
 * Dialect abstracting Hibernate OGM from the grid implementation.
 * <p>
 * Specific grid dialects may implement one or more <i>facet interfaces</i>, in order to provide additional
 * functionality such as querying or batching support. Rather than implementing this interface directly, it is recommend
 * to extend {@link BaseGridDialect}.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public interface GridDialect extends Service {

	/**
	 * Returns a {@link LockingStrategy} for locking the given lockable, using the given lock mode.
	 *
	 * @param lockable Static meta-data describing a lockable type
	 * @param lockMode A lock mode
	 * @return A locking strategy for the given lockable and lock mode or {@code null} if this dialect does not support
	 * the specified lock mode
	 */
	LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode);

	/**
	 * Return the tuple with the given column for a given key
	 *
	 * @param key The tuple identifier
	 * @param operationContext Contains additional information that might be used to create the tuple
	 * @return the tuple identified by the key
	 */
	Tuple getTuple(EntityKey key, OperationContext operationContext);

	/**
	 * Creates a new tuple for the given entity key.
	 * <p>
	 * Only invoked if no tuple is present yet for the given key. Implementations should not perform a round-trip to the
	 * datastore but rather return a transient instance. The OGM engine will invoke
	 * {@link #insertOrUpdateTuple(EntityKey, TuplePointer, TupleContext)} subsequently.
	 * <p>
	 * Columns in the tuple may represent properties of the corresponding entity as well as *-to-one associations to
	 * other entities. Implementations may choose to persist the latter e.g. in form of fields or as actual
	 * links/relationships to the element representing the associated entity. In case of multi-column keys, the
	 * corresponding association role for a given column can be obtained from the passed tuple context.
	 *
	 * @param key The tuple identifier
	 * @param operationContext Contains additional information that might be used to create the tuple
	 * @return the created tuple
	 */
	Tuple createTuple(EntityKey key, OperationContext operationContext);

	/**
	 * Inserts or updates the tuple corresponding to the given entity key.
	 *
	 * @param key The tuple identifier
	 * @param tuplePointer A pointer to the list of operations to execute
	 * @param tupleContext Contains additional information that might be used to create or update the tuple
	 * @throws TupleAlreadyExistsException upon insertion of a tuple with an already existing unique identifier
	 */
	void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) throws TupleAlreadyExistsException;

	/**
	 * Remove the tuple for a given key
	 *
	 * @param key The tuple identifier
	 * @param tupleContext Contains additional information that might be used to remove the tuple
	 */
	void removeTuple(EntityKey key, TupleContext tupleContext);

	/**
	 * Return the list of tuples corresponding to a given association and the given context
	 *
	 * @param key Identifies the association
	 * @param associationContext Contains additional information that might be used to get the association
	 * @return a list of tuples
	 */
	Association getAssociation(AssociationKey key, AssociationContext associationContext);

	/**
	 * Creates a new (empty) association for storing the tuples representing the rows corresponding to the given key.
	 * <p>
	 * Only invoked if the association does not yet exist in the datastore. Implementations should not perform a
	 * round-trip to the datastore but rather return a transient instance. The OGM engine will invoke
	 * {@link #insertOrUpdateAssociation(AssociationKey, Association, AssociationContext)} subsequently.
	 *
	 * @param key Identifies the association
	 * @param associationContext Contains additional information that might be used to create the association
	 * @return the created association
	 */
	Association createAssociation(AssociationKey key, AssociationContext associationContext);

	/**
	 * Inserts or updates the given association in the datastore.
	 *
	 * @param key Identifies the association
	 * @param association The list of operations to execute
	 * @param associationContext Contains additional information that might be used to create the association
	 */
	void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext);

	/**
	 * Remove the list of tuples corresponding to a given association
	 *
	 * @param key Identifies the association
	 * @param associationContext Contains additional information that might be used to remove an association
	 */
	void removeAssociation(AssociationKey key, AssociationContext associationContext);

	/**
	 * Whether the specified association is stored within an entity structure or not. E.g. dialects for document stores
	 * may support storing associations within entity documents and would have to return {@code true} if this is the
	 * case for a given association.
	 *
	 * @param associationKeyMetadata identifies the association of interest
	 * @param associationTypeContext provides additional contextual information about the represented association type,
	 * such as the options effectively applying for it
	 * @return {@code true} if the specified association is stored within an entity structure, {@code false} otherwise.
	 */
	boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext);

	/**
	 * Returns the next value from the specified id generator with the specified increment.
	 *
	 * @param request Identifies a specific id generator
	 * @return the next value from the specified id generator
	 */
	Number nextValue(NextValueRequest request);

	/**
	 * Whether this dialect supports sequences for id generation or not. If not, a table-based strategy will be used as
	 * fall-back.
	 *
	 * @return {@code true} if this dialect supports sequences, {@code false} otherwise.
	 */
	boolean supportsSequences();

	/**
	 * If the datastore does not support a {@link Type} the dialect might override it with a custom one.
	 *
	 * @param type The {@link Type} that might need to be overridden
	 * @return the GridType instance to use to bind the given {@code type} or null if the type does not need to be
	 * overridden
	 */
	@Experimental( "Custom types including the GridType contract will be re-visited after OGM 4.1.0.Final." )
	GridType overrideType(Type type);

	/**
	 * A consumer is called for each tuple matching the selected {@link EntityKeyMetadata}. The tuples must be of the
	 * same indexed type.
	 *
	 * @param consumer
	 *            the instance that is going to be called for every {@link Tuple}
	 * @param tupleTypeContext
	 *            contains additional information that might be used to build the tuple
	 * @param entityKeyMetadata
	 *            the key metadata of the table for which we want to apply the consumer
	 */
	void forEachTuple(ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata);

	/**
	 * Returns this dialect's strategy for detecting the insertion of several entity tuples of the given type with the
	 * same primary key.
	 *
	 * @param entityKeyMetadata meta-data identifying the entity type of interest
	 * @return This dialect's strategy for detecting the insertion of several entity tuples of the given type with the
	 * same primary key
	 */
	DuplicateInsertPreventionStrategy getDuplicateInsertPreventionStrategy(EntityKeyMetadata entityKeyMetadata);

	/**
	 * Whether this dialect uses navigational information to deal with the inverse side of an association.
	 *
	 * @return {@code true} is this dialect uses navigational information.
	 */
	boolean usesNavigationalInformationForInverseSideOfAssociations();
}
