/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.spi;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.Experimental;
import org.hibernate.type.ForeignKeyDirection;

/**
 * Defines a mapping between a Java type and one grid type, as well
 * as describing the in-memory semantics of the given java type (how do we check it for 'dirtiness', how do
 * we copy values, etc).
 * <p>
 * Application developers needing custom types can implement this interface (either directly or via subclassing an
 * existing impl) or by the (slightly more stable, though more limited) {@link org.hibernate.usertype.UserType}
 * interface.
 * <p>
 * Implementations of this interface must certainly be thread-safe.  It is recommended that they be immutable as
 * well, though that is difficult to achieve completely given the no-arg constructor requirement for custom types.
 *
 * @author Gavin King
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
@Experimental( "Custom types including the GridType contract will be re-visited after OGM 4.1.0.Final." )
public interface GridType extends Serializable {
	/**
	 * Return true if the implementation is castable to {@link org.hibernate.type.AssociationType}. This does not necessarily imply that
	 * the type actually represents an association.  Essentially a polymorphic version of
	 * {@code (type instanceof AssociationType.class)}
	 *
	 * @return True if this type is also an {@link org.hibernate.type.AssociationType} implementor; false otherwise.
	 */
	boolean isAssociationType();

	/**
	 * Return true if the implementation is castable to {@link org.hibernate.type.CollectionType}. Essentially a polymorphic version of
	 * {@code (type instanceof CollectionType.class)}
	 * <p>
	 * A {@link org.hibernate.type.CollectionType} is additionally an {@link org.hibernate.type.AssociationType}; so if this method returns true,
	 * {@link #isAssociationType()} should also return true.
	 *
	 * @return True if this type is also an {@link org.hibernate.type.CollectionType} implementor; false otherwise.
	 */
	boolean isCollectionType();

	/**
	 * Return true if the implementation is castable to {@link org.hibernate.type.EntityType}. Essentially a polymorphic
	 * version of {@code (type instanceof EntityType.class)}.
	 * <p>
	 * An {@link org.hibernate.type.EntityType} is additionally an {@link org.hibernate.type.AssociationType}; so if this method returns true,
	 * {@link #isAssociationType()} should also return true.
	 *
	 * @return True if this type is also an {@link org.hibernate.type.EntityType} implementor; false otherwise.
	 */
	boolean isEntityType();

	/**
	 * Return true if the implementation is castable to {@link org.hibernate.type.AnyType}. Essentially a polymorphic
	 * version of {@code (type instanceof AnyType.class)}.
	 * <p>
	 * An {@link org.hibernate.type.AnyType} is additionally an {@link org.hibernate.type.AssociationType}; so if this method returns true,
	 * {@link #isAssociationType()} should also return true.
	 *
	 * @return True if this type is also an {@link org.hibernate.type.AnyType} implementor; false otherwise.
	 */
	boolean isAnyType();

	/**
	 * Return true if the implementation is castable to {@link org.hibernate.type.CompositeType}. Essentially a polymorphic
	 * version of {@code (type instanceof CompositeType.class)}.  A component type may own collections or
	 * associations and hence must provide certain extra functionality.
	 *
	 * @return True if this type is also an {@link org.hibernate.type.CompositeType} implementor; false otherwise.
	 */
	boolean isComponentType();

	/**
	 * How many columns are used to persist this type.  Always the same as {@code sqlTypes(mapping).length}
	 *
	 * @param mapping The mapping object :/
	 *
	 * @return The number of columns
	 *
	 * @throws MappingException Generally indicates an issue accessing the passed mapping object.
	 */
	int getColumnSpan(Mapping mapping) throws MappingException;

	/**
	 * The class returned by {@link #nullSafeGet} methods. This is used to  establish the class of an array of
	 * this type.
	 *
	 * @return The java type class handled by this type.
	 */
	Class<?> getReturnedClass();

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality" (equality of persistent
	 * state) taking a shortcut for entity references.
	 * <p>
	 * For most types this should equate to {@link java.lang.Object#equals} check on the values.  For associations the implication
	 * is a bit different.  For most types it is conceivable to simply delegate to {@link #isEqual}
	 *
	 * @param x The first value
	 * @param y The second value
	 *
	 * @return True if there are considered the same (see discussion above).
	 *
	 * @throws org.hibernate.HibernateException A problem occurred performing the comparison
	 */
	boolean isSame(Object x, Object y) throws HibernateException;

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality" (equality of persistent
	 * state).
	 * <p>
	 * This should always equate to some form of comparison of the value's internal state.  As an example, for
	 * something like a date the comparison should be based on its internal "time" state based on the specific portion
	 * it is meant to represent (timestamp, date, time).
	 *
	 * @param x The first value
	 * @param y The second value
	 *
	 * @return True if there are considered equal (see discussion above).
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	boolean isEqual(Object x, Object y) throws HibernateException;

	/**
	 * Compare two instances of the class mapped by this type for persistence "equality" (equality of persistent
	 * state).
	 * <p>
	 * This should always equate to some form of comparison of the value's internal state.  As an example, for
	 * something like a date the comparison should be based on its internal "time" state based on the specific portion
	 * it is meant to represent (timestamp, date, time).
	 *
	 * @param x The first value
	 * @param y The second value
	 * @param factory The session factory
	 *
	 * @return True if there are considered equal (see discussion above).
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	boolean isEqual(Object x, Object y, SessionFactoryImplementor factory)
			throws HibernateException;

	/**
	 * Get a hash code, consistent with persistence "equality".  Again for most types the normal usage is to
	 * delegate to the value's {@link java.lang.Object#hashCode}.
	 *
	 * @param x The value for which to retrieve a hash code
	 *
	 * @return The hash code
	 *
	 * @throws HibernateException A problem occurred calculating the hash code
	 */
	int getHashCode(Object x) throws HibernateException;

	/**
	 * Get a hash code, consistent with persistence "equality".  Again for most types the normal usage is to
	 * delegate to the value's {@link java.lang.Object#hashCode}.
	 *
	 * @param x The value for which to retrieve a hash code
	 * @param factory The session factory
	 *
	 * @return The hash code
	 *
	 * @throws HibernateException A problem occurred calculating the hash code
	 */
	int getHashCode(Object x, SessionFactoryImplementor factory) throws HibernateException;

	/**
	 * Perform a {@link java.util.Comparator} style comparison between values
	 *
	 * @param x The first value
	 * @param y The second value
	 *
	 * @return The comparison result.  See {@link java.util.Comparator#compare} for a discussion.
	 */
	int compare(Object x, Object y);

	/**
	 * Should the parent be considered dirty, given both the old and current value?
	 *
	 * @param old the old value
	 * @param current the current value
	 * @param session The session from which the request originated.
	 *
	 * @return true if the field is dirty
	 *
	 * @throws HibernateException A problem occurred performing the checking
	 */
	boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) throws HibernateException;

	/**
	 * Should the parent be considered dirty, given both the old and current value?
	 *
	 * @param oldState the old value
	 * @param currentState the current value
	 * @param checkable An array of booleans indicating which columns making up the value are actually checkable
	 * @param session The session from which the request originated.
	 *
	 * @return true if the field is dirty
	 *
	 * @throws HibernateException A problem occurred performing the checking
	 */
	boolean isDirty(Object oldState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException;

	/**
	 * Has the value been modified compared to the current database state?  The difference between this
	 * and the {@link #isDirty} methods is that here we need to account for "partially" built values.  This is really
	 * only an issue with association types.  For most type implementations it is enough to simply delegate to
	 * {@link #isDirty} here/
	 *
	 * @param dbState the database state, in a "hydrated" form, with identifiers unresolved
	 * @param currentState the current state of the object
	 * @param checkable which columns are actually updatable
	 * @param session The session from which the request originated.
	 *
	 * @return true if the field has been modified
	 *
	 * @throws HibernateException A problem occurred performing the checking
	 */
	boolean isModified(Object dbState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException;

	/**
	 * Retrieve an instance of the mapped class from a grid resultset. Implementors
	 * should handle possibility of null values.
	 *
	 * @see GridType#hydrate(Tuple, String[], SharedSessionContractImplementor, Object) alternative, 2-phase property initialization
	 * @param rs the resultset
	 * @param names the column names
	 * @param session the session
	 * @param owner the parent entity
	 * @return the instance of the mapped class from a grid resultset
	 *
	 * @throws HibernateException if an error occurs while retrievin the instance
	 */
	Object nullSafeGet(Tuple rs, String[] names, SharedSessionContractImplementor session, Object owner)
	throws HibernateException;

	/**
	 * Retrieve an instance of the mapped class from a grid resultset. Implementations
	 * should handle possibility of null values. This method might be called if the
	 * type is known to be a single-column type.
	 *
	 * @see GridType#hydrate(Tuple, String[], SharedSessionContractImplementor, Object) alternative, 2-phase property initialization
	 * @param rs the resultset
	 * @param name the column name
	 * @param session the session
	 * @param owner the parent entity
	 * @return an instance of the mapped class
	 *
	 * @throws HibernateException if an error occurs retrieving the instance
	 */
	Object nullSafeGet(Tuple rs, String name, SharedSessionContractImplementor session, Object owner)
	throws HibernateException;

	/**
	 * Write an instance of the mapped class to a prepared statement, ignoring some columns.
	 * Implementors should handle possibility of null values. A multi-column type should be
	 * written to parameters starting from <tt>index</tt>.
	 *
	 * @param resultset to push value into
	 * @param value the object to write
	 * @param names of affected columns
	 * @param settable an array indicating which columns to ignore
	 * @param session the session
	 *
	 * @throws HibernateException if an error occurs writing the value
	 */
	void nullSafeSet(Tuple resultset, Object value, String[] names, boolean[] settable, SharedSessionContractImplementor session)
	throws HibernateException;

	/**
	 * Write an instance of the mapped class to a prepared statement. Implementors
	 * should handle possibility of null values. A multi-column type should be written
	 * to parameters starting from <tt>index</tt>.
	 *
	 * @param resultset to push value into
	 * @param value the object to write
	 * @param names of affected columns
	 * @param session the session
	 *
	 * @throws HibernateException if an error occurs writing the value
	 */
	void nullSafeSet(Tuple resultset, Object value, String[] names, SharedSessionContractImplementor session)
	throws HibernateException;

	/**
	 * Returns the value which has been stored in the datastore so that we can perform queries on it.
	 *
	 * @param value the object we use in the query
	 * @param sessionFactory the sessionFactory
	 * @return the object as stored in the database
	 */
	Object convertToBackendType(Object value, SessionFactoryImplementor sessionFactory);

	/**
	 * A representation of the value to be embedded in a log file.
	 *
	 * @param value the object to convert into a string to log
	 * @param factory the session factory
	 * @return a {@link String} representation of the value
	 *
	 * @throws HibernateException if an error occurs during the conversion
	 */
	String toLoggableString(Object value, SessionFactoryImplementor factory)
	throws HibernateException;

	/**
	 * Returns the abbreviated name of the type.
	 *
	 * @return the Hibernate type name as {@link String}
	 */
	String getName();

	/**
	 * Return a deep copy of the persistent state, stopping at entities and at
	 * collections.
	 *
	 * @param value generally a collection element or entity field
	 * @param factory the session factory
	 * @return Object a copy
	 *
	 * @throws HibernateException if an error occurs
	 */
	Object deepCopy(Object value, SessionFactoryImplementor factory)
	throws HibernateException;

	/**
	 * Are objects of this type mutable. (With respect to the referencing object ...
	 * entities and collections are considered immutable because they manage their
	 * own internal state.)
	 *
	 * @return {@code true} if mutable, false otherwise
	 */
	boolean isMutable();

	/**
	 * Return a cacheable "disassembled" representation of the object.
	 *
	 * @param value the value to cache
	 * @param session the session
	 * @param owner optional parent entity object (needed for collections)
	 * @return the disassembled, deep cloned state
	 */
	Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException;

	/**
	 * Reconstruct the object from its cached "disassembled" state.
	 *
	 * @param cached the disassembled state from the cache
	 * @param session the session
	 * @param owner the parent entity object
	 * @return owner the assembled object
	 */
	Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner)
	throws HibernateException;

	/**
	 * Called before assembling a query result set from the query cache, to allow batch fetching
	 * of entities missing from the second-level cache.
	 *
	 * @param cached the cached result set
	 * @param session the session
	 */
	void beforeAssemble(Serializable cached, SharedSessionContractImplementor session);

	/**
	 * Retrieve an instance of the mapped class, or the identifier of an entity or collection,
	 * from a grid resultset. This is useful for 2-phase property initialization - the second
	 * phase is a call to <tt>resolveIdentifier()</tt>.
	 *
	 * @see #resolve(Object, SharedSessionContractImplementor, Object)
	 * @param rs the result set
	 * @param names the column names
	 * @param session the session
	 * @param owner the parent entity
	 * @return Object an identifier or actual value
	 *
	 * @throws HibernateException if an error occurs
	 */
	Object hydrate(Tuple rs, String[] names, SharedSessionContractImplementor session, Object owner)
	throws HibernateException;

	/**
	 * Map identifiers to entities or collections. This is the second phase of 2-phase property
	 * initialization.
	 *
	 * @see GridType#hydrate(Tuple, String[], SharedSessionContractImplementor, Object)
	 * @param value an identifier or value returned by <tt>hydrate()</tt>
	 * @param owner the parent entity
	 * @param session the session
	 * @return the given value, or the value associated with the identifier
	 *
	 * @throws HibernateException if an error occurs
	 */
	Object resolve(Object value, SharedSessionContractImplementor session, Object owner)
	throws HibernateException;

	/**
	 * Given a hydrated, but unresolved value, return a value that may be used to
	 * reconstruct property-ref associations.
	 *
	 * @param value the unresolved value
	 * @param session the session
	 * @param owner the owner of the association
	 * @return a value that may be used to reconstruct property-ref associations
	 *
	 * @throws HibernateException if an error occurs
	 */
	Object semiResolve(Object value, SharedSessionContractImplementor session, Object owner)
	throws HibernateException;

	/**
	 * Get the type of a semi-resolved value.
	 *
	 * @param factory the session factory
	 * @return the {@link GridType} of the semi resolve value
	 */
	GridType getSemiResolvedType(SessionFactoryImplementor factory);

	/**
	 * During merge, replace the existing (target) value in the entity we are merging to
	 * with a new (original) value from the detached entity we are merging. For immutable
	 * objects, or null values, it is safe to simply return the first parameter. For
	 * mutable objects, it is safe to return a copy of the first parameter. For objects
	 * with component values, it might make sense to recursively replace component values.
	 *
	 * @param original the value from the detached entity being merged
	 * @param target the value in the managed entity
	 * @param session the session
	 * @param owner the owner entity
	 * @param copyCache the cache of already copied/replaced values
	 * @return the value to be merged
	 *
	 * @throws HibernateException if an error occurs
	 */
	Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache)
	throws HibernateException;

	/**
	 * During merge, replace the existing (target) value in the entity we are merging to
	 * with a new (original) value from the detached entity we are merging. For immutable
	 * objects, or null values, it is safe to simply return the first parameter. For
	 * mutable objects, it is safe to return a copy of the first parameter. For objects
	 * with component values, it might make sense to recursively replace component values.
	 *
	 * @param original the value from the detached entity being merged
	 * @param target the value in the managed entity
	 * @param session the sesison
	 * @param owner the owner entity
	 * @param copyCache the cache of already copied/replaced values
	 * @param foreignKeyDirection the directionality of the foreign key constraint
	 * @return the value to be merged
	 *
	 * @throws HibernateException if an error occurs
	 */
	Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection)
	throws HibernateException;

	/**
	 * Given an instance of the type, return an array of boolean, indicating which mapped columns would be null.
	 *
	 * @param value an instance of the type
	 * @param mapping the session factory
	 * @return an array of {@code boolean}s, each value is associated to the corresponding column and it's {@code true}
	 * if the column can be null, {@code false} otherwise
	 */
	boolean[] toColumnNullness(Object value, Mapping mapping);

}
