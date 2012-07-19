/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.mapping.impl;

import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.ogm.mapping.context.EntityContext;
import org.hibernate.ogm.mapping.context.GlobalContext;
import org.hibernate.ogm.mapping.context.PropertyContext;

import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Bind the expected generator implementation with the
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MappingToGeneratorInvocationHandler implements InvocationHandler {
	private static final Method ENTITIES_FROM_GLOBAL;
	private static final Method ENTITIES_FROM_ENTITY;
	private static final Method ENTITIES_FROM_PROPERTY;
	private static final Method PROPERTY_FROM_ENTITY;
	private static final Method PROPERTY_FROM_PROPERTY;

	static {
		try {
			ENTITIES_FROM_GLOBAL = GlobalContext.class.getMethod( "entity", Class.class );
			ENTITIES_FROM_ENTITY = EntityContext.class.getMethod( "entity", Class.class );
			ENTITIES_FROM_PROPERTY = PropertyContext.class.getMethod( "entity", Class.class );
			PROPERTY_FROM_ENTITY = EntityContext.class.getMethod( "property", String.class, ElementType.class );
			PROPERTY_FROM_PROPERTY = PropertyContext.class.getMethod( "property", String.class, ElementType.class );
		} catch ( NoSuchMethodException e ) {
			throw new AssertionFailure("Unable to load GlobalContext, EntityContext or PropertyContext methods");
		}
	}

	private final MappingContext context;

	public MappingToGeneratorInvocationHandler(MappingContext context) {
		this.context = context;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		//need ArrayList as arrays are compared by == and not by elements identity
		List<Object> key;
		if ( isEqual(method, ENTITIES_FROM_GLOBAL)
				|| isEqual(method, ENTITIES_FROM_ENTITY)
				|| isEqual(method, ENTITIES_FROM_PROPERTY) ) {
			// we could use a typed approach but since we only have entities and properties that will suffice
			// { method, propertyName, elementType }
			key = new ArrayList<Object>(2);
			key.add( method );
			key.add( args[0] );
			return returnProxy( key, context.getEntityContextClass() );
		}
		else if ( isEqual(method, PROPERTY_FROM_ENTITY)
						|| isEqual(method, PROPERTY_FROM_PROPERTY) ) {
			// we could use a typed approach but since we only have entities and properties that will suffice
			// { method, propertyName, elementType }
			key = new ArrayList<Object>(3);
			key.add( method );
			key.add( args[0] );
			key.add( args[1] );
			return returnProxy( key, context.getPropertyContextClass() );
		}

		Object delegate = context.getFromInterfaceToGenerator().get( method );
		if ( delegate != null ) {
			//TODO store that info somewhere
			method.invoke( delegate, args );
		}
		//if nothing match we return the proxy directly?
		//FIXME or should we rather raise an exception?
		return proxy;
	}

	private Object returnProxy(List<Object> key, Class<?> contextClass) {
		Object result = context.getProxyPerEntityOrProperty().get( key );
		if (result == null) {
			result = ConfigurationProxyFactory.get( contextClass, context );
			context.getProxyPerEntityOrProperty().put( key, result );
		}
		return result;
	}

	private boolean isEqual(Method from, Method to) {
		return from == to ? true: from.equals( to );
	}
}
