package org.hibernate.ogm.type;

import java.io.Serializable;
import java.util.Map;

import org.dom4j.Node;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.ogm.type.descriptor.GridTypeDescriptor;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.StringRepresentableType;
import org.hibernate.type.XmlRepresentableType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.util.ArrayHelper;

/**
 * Not a public API
 * Copied from {@link org.hibernate.type.AbstractStandardBasicType}
 * 
 * @author Emmanuel Bernard
 */
public abstract class AbstractGenericBasicType<T>
		implements  GridType, //BasicType,
				StringRepresentableType<T>, XmlRepresentableType<T> {

	private final GridTypeDescriptor gridTypeDescriptor;
	private final JavaTypeDescriptor<T> javaTypeDescriptor;

	public AbstractGenericBasicType(GridTypeDescriptor gridTypeDescriptor, JavaTypeDescriptor<T> javaTypeDescriptor) {
		this.gridTypeDescriptor = gridTypeDescriptor;
		this.javaTypeDescriptor = javaTypeDescriptor;
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
		return value == null ? ArrayHelper.FALSE : ArrayHelper.TRUE;
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

	public final boolean isSame(Object x, Object y, EntityMode entityMode) {
		return isSame( x, y );
	}

	@SuppressWarnings({ "unchecked" })
	protected final boolean isSame(Object x, Object y) {
		return isEqual( (T) x, (T) y );
	}

	@SuppressWarnings({ "unchecked" })
	public final boolean isEqual(Object x, Object y, EntityMode entityMode) {
		return isEqual( (T) x, (T) y );
	}

	@SuppressWarnings({ "unchecked" })
	public final boolean isEqual(Object x, Object y, EntityMode entityMode, SessionFactoryImplementor factory) {
		return isEqual( (T) x, (T) y );
	}

	@SuppressWarnings({ "unchecked" })
	public final boolean isEqual(T one, T another) {
		return javaTypeDescriptor.areEqual( one, another );
	}

	public final int getHashCode(Object x, EntityMode entityMode) {
		return getHashCode( x );
	}

	public final int getHashCode(Object x, EntityMode entityMode, SessionFactoryImplementor factory) {
		return getHashCode( x );
	}

	@SuppressWarnings({ "unchecked" })
	protected final int getHashCode(Object x) {
		return javaTypeDescriptor.extractHashCode( (T) x );
	}

	@SuppressWarnings({ "unchecked" })
	public final int compare(Object x, Object y, EntityMode entityMode) {
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

	public final Object nullSafeGet(
			Map<String,Object> rs,
			String[] names,
			SessionImplementor session,
			Object owner) {
		return nullSafeGet( rs, names[0], session );
	}

	public final Object nullSafeGet(Map<String,Object> rs, String name, SessionImplementor session, Object owner) {
		return nullSafeGet( rs, name, session );
	}

	public final T nullSafeGet(Map<String,Object> rs, String name, final SessionImplementor session) {
		// todo : have SessionImplementor extend WrapperOptions
		final WrapperOptions options = new WrapperOptions() {
			public boolean useStreamForLobBinding() {
				return Environment.useStreamsForBinary();
			}

			public LobCreator getLobCreator() {
				return Hibernate.getLobCreator( session );
			}
		};

		return nullSafeGet( rs, name, options );
	}

	protected final T nullSafeGet(Map<String,Object> rs, String name, WrapperOptions options) {
		return gridTypeDescriptor.getExtractor( javaTypeDescriptor ).extract( rs, name );
	}

//	public Object get(Map<String,Object> rs, String name, SessionImplementor session) throws HibernateException, SQLException {
//		return nullSafeGet( rs, name, session );
//	}

	@SuppressWarnings({ "unchecked" })
	public final void nullSafeSet(
			Map<String,Object> rs,
			Object value,
			String name,
			final SessionImplementor session)  {
		// todo : have SessionImplementor extend WrapperOptions
		final WrapperOptions options = new WrapperOptions() {
			public boolean useStreamForLobBinding() {
				return Environment.useStreamsForBinary();
			}

			public LobCreator getLobCreator() {
				return Hibernate.getLobCreator( session );
			}
		};

		nullSafeSet( rs, value, name, options );
	}

	@SuppressWarnings({ "unchecked" })
	protected final void nullSafeSet(Map<String,Object> rs, Object value, String name, WrapperOptions options) {
		gridTypeDescriptor.getBinder( javaTypeDescriptor ).bind( rs, (T) value, name );
	}

	public final void nullSafeSet(Map<String, Object> st, Object value, String name, boolean[] settable, SessionImplementor session)
			throws HibernateException {
		if ( settable[0] ) {
			nullSafeSet( st, value, name, session );
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

	@SuppressWarnings({ "unchecked" })
	public final Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory) {
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

	public final Object hydrate(Map<String,Object> rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		return nullSafeGet(rs, names, session, owner);
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
