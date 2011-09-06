/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.type;

import java.io.Serializable;
import java.util.Map;

import org.dom4j.Node;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
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
	public boolean isSame(Object x, Object y) throws HibernateException {
		return delegate.isSame( x, y );
	}

	@Override
	public boolean isEqual(Object x, Object y) throws HibernateException {
		return delegate.isEqual( x, y );
	}

	@Override
	public boolean isEqual(Object x, Object y, SessionFactoryImplementor factory)
			throws HibernateException {
		return delegate.isEqual( x, y, factory );
	}

	@Override
	public int getHashCode(Object x) throws HibernateException {
		return delegate.getHashCode( x );
	}

	@Override
	public int getHashCode(Object x, SessionFactoryImplementor factory)
			throws HibernateException {
		return delegate.getHashCode( x, factory );
	}

	@Override
	public int compare(Object x, Object y) {
		return delegate.compare( x, y );
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
	public Object deepCopy(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		return delegate.deepCopy( value, factory );
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
