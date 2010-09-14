/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ogm.type;

import java.io.Serializable;
import java.util.Map;

import org.dom4j.Node;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;

/**
 * Delegates non persistence operations to the underlying Hibernate Core Type.
 *
 * @author Emmanuel Bernard
 */
public abstract class GridTypeDelegatingToCoreType implements GridType {
	private final Type delegate;

	GridTypeDelegatingToCoreType(Type type) {
		this.delegate = type;
	}

	@Override
	public boolean isAssociationType() {
		return delegate.isAssociationType();
	}

	@Override
	public boolean isCollectionType() {
		return delegate.isCollectionType();
	}

	@Override
	public boolean isEntityType() {
		return delegate.isEntityType();
	}

	@Override
	public boolean isAnyType() {
		return delegate.isAnyType();
	}

	@Override
	public boolean isComponentType() {
		return delegate.isComponentType();
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return delegate.getColumnSpan( mapping );
	}

	@Override
	public Class getReturnedClass() {
		return delegate.getReturnedClass();
	}

	@Override
	public boolean isXMLElement() {
		return delegate.isXMLElement();
	}

	@Override
	public boolean isSame(Object x, Object y, EntityMode entityMode) throws HibernateException {
		return delegate.isSame( x, y, entityMode );
	}

	@Override
	public boolean isEqual(Object x, Object y, EntityMode entityMode) throws HibernateException {
		return delegate.isEqual( x, y, entityMode );
	}

	@Override
	public boolean isEqual(Object x, Object y, EntityMode entityMode, SessionFactoryImplementor factory)
			throws HibernateException {
		return delegate.isEqual( x, y, entityMode, factory );
	}

	@Override
	public int getHashCode(Object x, EntityMode entityMode) throws HibernateException {
		return delegate.getHashCode( x, entityMode );
	}

	@Override
	public int getHashCode(Object x, EntityMode entityMode, SessionFactoryImplementor factory)
			throws HibernateException {
		return delegate.getHashCode( x, entityMode, factory );
	}

	@Override
	public int compare(Object x, Object y, EntityMode entityMode) {
		return delegate.compare( x, y, entityMode );
	}

	@Override
	public boolean isDirty(Object old, Object current, SessionImplementor session) throws HibernateException {
		return delegate.isDirty( old, current, session );
	}

	@Override
	public boolean isDirty(Object oldState, Object currentState, boolean[] checkable, SessionImplementor session)
			throws HibernateException {
		return delegate.isDirty( oldState, currentState, checkable, session );
	}

	@Override
	public boolean isModified(Object dbState, Object currentState, boolean[] checkable, SessionImplementor session)
			throws HibernateException {
		return delegate.isModified( dbState, currentState, checkable, session );
	}

	@Override
	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) throws HibernateException {
		delegate.setToXMLNode( node, value, factory );
	}

	@Override
	public String toLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		return delegate.toLoggableString( value, factory );
	}

	@Override
	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		return delegate.fromXMLNode( xml, factory );
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory)
			throws HibernateException {
		return delegate.deepCopy( value, entityMode, factory );
	}

	@Override
	public boolean isMutable() {
		return delegate.isMutable();
	}

	@Override
	public Serializable disassemble(Object value, SessionImplementor session, Object owner) throws HibernateException {
		return delegate.disassemble( value, session, owner );
	}

	@Override
	public Object assemble(Serializable cached, SessionImplementor session, Object owner) throws HibernateException {
		return delegate.assemble( cached, session, owner );
	}

	@Override
	public void beforeAssemble(Serializable cached, SessionImplementor session) {
		delegate.beforeAssemble( cached, session );
	}

	@Override
	public Object resolve(Object value, SessionImplementor session, Object owner) throws HibernateException {
		return delegate.resolve( value, session, owner );
	}

	@Override
	public Object semiResolve(Object value, SessionImplementor session, Object owner) throws HibernateException {
		return delegate.semiResolve( value, session, owner );
	}

	@Override
	public GridType getSemiResolvedType(SessionFactoryImplementor factory) {
		return this;
	}

	@Override
	public Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache)
			throws HibernateException {
		return delegate.replace( original, target, session, owner, copyCache );
	}

	@Override
	public Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache, ForeignKeyDirection foreignKeyDirection)
			throws HibernateException {
		return delegate.replace( original, target, session, owner, copyCache, foreignKeyDirection );
	}

	@Override
	public boolean[] toColumnNullness(Object value, Mapping mapping) {
		return delegate.toColumnNullness( value, mapping );
	}
}
