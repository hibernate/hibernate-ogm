/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.StringRepresentableType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * Not a public API
 * Copied from {@link org.hibernate.type.AbstractStandardBasicType}
 *
 * @author Emmanuel Bernard
 */
public abstract class AbstractGenericBasicType<T>
		implements  GridType, //BasicType,
				StringRepresentableType<T> {

	private static final boolean[] TRUE = { true };
	private static final boolean[] FALSE = { false };

	private final GridTypeDescriptor gridTypeDescriptor;
	private final JavaTypeDescriptor<T> javaTypeDescriptor;
	private final GridValueExtractor<T> typeExtractor;
	private final GridValueBinder<T> typeBinder;

	public AbstractGenericBasicType(GridTypeDescriptor gridTypeDescriptor, JavaTypeDescriptor<T> javaTypeDescriptor) {
		this.gridTypeDescriptor = gridTypeDescriptor;
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.typeExtractor = gridTypeDescriptor.getExtractor( javaTypeDescriptor );
		this.typeBinder = gridTypeDescriptor.getBinder( javaTypeDescriptor );
	}

	public T fromString(String string) {
		return javaTypeDescriptor.fromString( string );
	}

	@Override
	public String toString(T value) {
		return javaTypeDescriptor.toString( value );
	}

	@Override
	public T fromStringValue(String xml) throws HibernateException {
		return fromString( xml );
	}

	protected MutabilityPlan<T> getMutabilityPlan() {
		return javaTypeDescriptor.getMutabilityPlan();
	}

	protected T getReplacement(T original, T target) {
		if ( !isMutable() ) {
			return original;
		}
		else if ( isEqual( original, target ) ) {
			return original;
		}
		else {
			return deepCopy( original );
		}
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return value == null ? FALSE : TRUE;
	}

	public String[] getRegistrationKeys() {
		return registerUnderJavaType()
				? new String[] { getName(), javaTypeDescriptor.getJavaTypeClass().getName() }
				: new String[] { getName() };
	}

	protected boolean registerUnderJavaType() {
		return false;
	}


	// final implementations ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public final JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	public final GridTypeDescriptor getGridTypeDescriptor() {
		return gridTypeDescriptor;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public final Class getReturnedClass() {
		return javaTypeDescriptor.getJavaTypeClass();
	}

	@Override
	public abstract int getColumnSpan(Mapping mapping) throws MappingException;

	@Override
	public final boolean isAssociationType() {
		return false;
	}

	@Override
	public final boolean isCollectionType() {
		return false;
	}

	@Override
	public final boolean isComponentType() {
		return false;
	}

	@Override
	public final boolean isEntityType() {
		return false;
	}

	@Override
	public final boolean isAnyType() {
		return false;
	}

	@Override
	public final boolean isSame(Object x, Object y) {
		return isEqual( x, y );
	}

	@Override
	public final boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		return isEqual( x, y );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public final boolean isEqual(Object one, Object another) {
		return javaTypeDescriptor.areEqual( (T) one, (T) another );
	}

	@Override
	public final int getHashCode(Object x, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public final int getHashCode(Object x) {
		return javaTypeDescriptor.extractHashCode( (T) x );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public final int compare(Object x, Object y) {
		return javaTypeDescriptor.getComparator().compare( (T) x, (T) y );
	}

	@Override
	public final boolean isDirty(Object old, Object current, SharedSessionContractImplementor session) {
		return isDirty( old, current );
	}

	@Override
	public final boolean isDirty(Object old, Object current, boolean[] checkable, SharedSessionContractImplementor session) {
		return checkable[0] && isDirty( old, current );
	}

	protected final boolean isDirty(Object old, Object current) {
		return !isSame( old, current );
	}

	@Override
	public final boolean isModified(
			Object oldHydratedState,
			Object currentState,
			boolean[] checkable,
			SharedSessionContractImplementor session) {
		return isDirty( oldHydratedState, currentState );
	}

	@Override
	public final Object nullSafeGet(
			Tuple rs,
			String[] names,
			SharedSessionContractImplementor session,
			Object owner) {
		return nullSafeGet( rs, names[0], session );
	}

	@Override
	public final Object nullSafeGet(Tuple rs, String name, SharedSessionContractImplementor session, Object owner) {
		return nullSafeGet( rs, name, session );
	}

	private T nullSafeGet(Tuple rs, String name, final SharedSessionContractImplementor session) {
		return nullSafeGet( rs, name, (WrapperOptions) null );
	}

	protected final T nullSafeGet(Tuple rs, String name, WrapperOptions options) {
		return typeExtractor.extract( rs, name );
	}

//	public Object get(Map<String,Object> rs, String name, SessionImplementor session) throws HibernateException, SQLException {
//		return nullSafeGet( rs, name, session );
//	}

	@Override
	public final void nullSafeSet(
			Tuple rs,
			Object value,
			String[] names,
			final SharedSessionContractImplementor session)  {
		nullSafeSet( rs, value, names, (WrapperOptions) null );
	}

	@SuppressWarnings({ "unchecked" })
	protected final void nullSafeSet(Tuple rs, Object value, String[] names, WrapperOptions options) {
		typeBinder.bind( rs, (T) value, names );
	}

	@Override
	public final void nullSafeSet(Tuple st, Object value, String[] names, boolean[] settable, SharedSessionContractImplementor session)
			throws HibernateException {
		if ( settable.length > 1 ) {
			throw new NotYetImplementedException( "Multi column property not implemented yet" );
		}
		if ( settable[0] ) {
			nullSafeSet( st, value, names, session );
		}
	}

	@Override
	public Object convertToBackendType(Object value, SessionFactoryImplementor sessionFactory) {
		Tuple tuple = new Tuple();
		nullSafeSet( tuple, value, new String[] { "dummy" }, (SessionImplementor) null );
		return tuple.get( "dummy" );
	}

//	public void set(PreparedStatement st, T value, int index, SessionImplementor session) throws HibernateException, SQLException {
//		nullSafeSet( st, value, index, session );
//	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return javaTypeDescriptor.extractLoggableRepresentation( (T) value );
	}

	@Override
	public final boolean isMutable() {
		return getMutabilityPlan().isMutable();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Object deepCopy(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return deepCopy( (T) value );
	}

	protected final T deepCopy(T value) {
		return getMutabilityPlan().deepCopy( value );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public final Serializable disassemble(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().disassemble( (T) value );
	}

	@Override
	public final Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().assemble( cached );
	}

	@Override
	public final void beforeAssemble(Serializable cached, SharedSessionContractImplementor session) {
	}

	@Override
	public final Object hydrate(Tuple rs, String[] names, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return nullSafeGet( rs, names, session, owner );
	}

	@Override
	public final Object resolve(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return value;
	}

	@Override
	public final Object semiResolve(Object value, SharedSessionContractImplementor session, Object owner) throws HibernateException {
		return value;
	}

	@Override
	public final GridType getSemiResolvedType(SessionFactoryImplementor factory) {
		return this;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final Object replace(Object original, Object target, SharedSessionContractImplementor session, Object owner, Map copyCache) {
		return getReplacement( (T) original, (T) target );
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object replace(
			Object original,
			Object target,
			SharedSessionContractImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection) {
		return ForeignKeyDirection.FROM_PARENT == foreignKeyDirection
				? getReplacement( (T) original, (T) target )
				: target;
	}
}
