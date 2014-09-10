/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import java.io.Serializable;
import java.util.Map;

import org.dom4j.Node;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.descriptor.impl.GridTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.GridValueBinder;
import org.hibernate.ogm.type.descriptor.impl.GridValueExtractor;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.StringRepresentableType;
import org.hibernate.type.XmlRepresentableType;
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
				StringRepresentableType<T>, XmlRepresentableType<T> {

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

	public String toString(T value) {
		return javaTypeDescriptor.toString( value );
	}

	public T fromStringValue(String xml) throws HibernateException {
		return fromString( xml );
	}

	public String toXMLString(T value, SessionFactoryImplementor factory) throws HibernateException {
		return toString( value );
	}

	public T fromXMLString(String xml, Mapping factory) throws HibernateException {
		return xml == null || xml.length() == 0 ? null : fromStringValue( xml );
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

	public final Class getReturnedClass() {
		return javaTypeDescriptor.getJavaTypeClass();
	}

	public abstract int getColumnSpan(Mapping mapping) throws MappingException;

	public final boolean isAssociationType() {
		return false;
	}

	public final boolean isCollectionType() {
		return false;
	}

	public final boolean isComponentType() {
		return false;
	}

	public final boolean isEntityType() {
		return false;
	}

	public final boolean isAnyType() {
		return false;
	}

	public final boolean isXMLElement() {
		return false;
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public final boolean isSame(Object x, Object y) {
		return isEqual( (T) x, (T) y );
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public final boolean isEqual(Object x, Object y, SessionFactoryImplementor factory) {
		return isEqual( (T) x, (T) y );
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

	public final boolean isDirty(Object old, Object current, SessionImplementor session) {
		return isDirty( old, current );
	}

	public final boolean isDirty(Object old, Object current, boolean[] checkable, SessionImplementor session) {
		return checkable[0] && isDirty( old, current );
	}

	protected final boolean isDirty(Object old, Object current) {
		return !isSame( old, current );
	}

	public final boolean isModified(
			Object oldHydratedState,
			Object currentState,
			boolean[] checkable,
			SessionImplementor session) {
		return isDirty( oldHydratedState, currentState );
	}

	@Override
	public final Object nullSafeGet(
			Tuple rs,
			String[] names,
			SessionImplementor session,
			Object owner) {
		return nullSafeGet( rs, names[0], session );
	}

	@Override
	public final Object nullSafeGet(Tuple rs, String name, SessionImplementor session, Object owner) {
		return nullSafeGet( rs, name, session );
	}

	private T nullSafeGet(Tuple rs, String name, final SessionImplementor session) {
		return nullSafeGet( rs, name, (WrapperOptions) null );
	}

	protected final T nullSafeGet(Tuple rs, String name, WrapperOptions options) {
		return typeExtractor.extract( rs, name );
	}

//	public Object get(Map<String,Object> rs, String name, SessionImplementor session) throws HibernateException, SQLException {
//		return nullSafeGet( rs, name, session );
//	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public final void nullSafeSet(
			Tuple rs,
			Object value,
			String[] names,
			final SessionImplementor session)  {
		nullSafeSet( rs, value, names, (WrapperOptions) null );
	}

	@SuppressWarnings({ "unchecked" })
	protected final void nullSafeSet(Tuple rs, Object value, String[] names, WrapperOptions options) {
		typeBinder.bind( rs, (T) value, names );
	}

	@Override
	public final void nullSafeSet(Tuple st, Object value, String[] names, boolean[] settable, SessionImplementor session)
			throws HibernateException {
		if (settable.length > 1) {
			throw new NotYetImplementedException("Multi column property not implemented yet");
		}
		if ( settable[0] ) {
			nullSafeSet( st, value, names, session );
		}
	}

//	public void set(PreparedStatement st, T value, int index, SessionImplementor session) throws HibernateException, SQLException {
//		nullSafeSet( st, value, index, session );
//	}

	@SuppressWarnings({ "unchecked" })
	public final String toLoggableString(Object value, SessionFactoryImplementor factory) {
		return javaTypeDescriptor.extractLoggableRepresentation( (T) value );
	}

	@SuppressWarnings({ "unchecked" })
	public final void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) {
		node.setText( toString( (T) value ) );
	}

	public final Object fromXMLNode(Node xml, Mapping factory) {
		return fromString( xml.getText() );
	}

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

	@SuppressWarnings({ "unchecked" })
	public final Serializable disassemble(Object value, SessionImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().disassemble( (T) value );
	}

	public final Object assemble(Serializable cached, SessionImplementor session, Object owner) throws HibernateException {
		return getMutabilityPlan().assemble( cached );
	}

	public final void beforeAssemble(Serializable cached, SessionImplementor session) {
	}

	@Override
	public final Object hydrate(Tuple rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		return nullSafeGet( rs, names, session, owner );
	}

	public final Object resolve(Object value, SessionImplementor session, Object owner) throws HibernateException {
		return value;
	}

	public final Object semiResolve(Object value, SessionImplementor session, Object owner) throws HibernateException {
		return value;
	}

	public final GridType getSemiResolvedType(SessionFactoryImplementor factory) {
		return this;
	}

	@SuppressWarnings({ "unchecked" })
	public final Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache) {
		return getReplacement( (T) original, (T) target );
	}

	@SuppressWarnings({ "unchecked" })
	public Object replace(
			Object original,
			Object target,
			SessionImplementor session,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection) {
		return ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT == foreignKeyDirection
				? getReplacement( (T) original, (T) target )
				: target;
	}
}
