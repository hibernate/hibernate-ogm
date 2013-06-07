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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Generate the appropriate proxy for the expected mapping object.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ConfigurationProxyFactory {
	private static final Log log = LoggerFactory.make();

	public static final <T> T get(Class<T> configurationType, MappingContext context) {
		try {
			validate( configurationType );
			return getProxyInstance( configurationType, context );
		}
		catch ( InstantiationException e ) {
			throw log.cannotCreateNewProxyInstance( e );
		}
		catch ( IllegalAccessException e ) {
			throw log.cannotCreateNewProxyInstance( e );
		}
		catch ( InvocationTargetException e ) {
			throw log.cannotCreateNewProxyInstance( e );
		}
		catch ( NoSuchMethodException e ) {
			throw log.cannotCreateNewProxyInstance( e );
		}
	}

	private static <T> void validate(Class<T> configurationType) {
		// find the expected type?
		// create the proxy mapping the type
		// implement the interceptor logic
		if ( !configurationType.isInterface() ) {
			throw log.mappingSubtypeNotInterface( configurationType );
		}
	}

	private static <T> T getProxyInstance(Class<T> configurationType, MappingContext context) throws InstantiationException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		Class<T> proxyClass = (Class<T>) Proxy.getProxyClass( configurationType.getClassLoader(), configurationType );
		InvocationHandler handler = new MappingToGeneratorInvocationHandler( context );
		return newInstance( proxyClass, handler );
	}

	private static <T> T newInstance(Class<T> proxyClass, InvocationHandler handler) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException {
		Constructor<T> constructor = proxyClass.getConstructor( InvocationHandler.class );
		return constructor.newInstance( handler );
	}

}
