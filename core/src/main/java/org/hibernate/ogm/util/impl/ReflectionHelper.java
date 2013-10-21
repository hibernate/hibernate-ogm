/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.util.impl;

import java.beans.Introspector;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Some reflection utility methods.
 *
 * @author Hardy Ferentschik
 * @author Gunnar Morling
 */
public final class ReflectionHelper {

	private static final String PROPERTY_ACCESSOR_PREFIX_GET = "get";
	private static final String PROPERTY_ACCESSOR_PREFIX_IS = "is";

	/**
	 * Whether the specified JavaBeans property exists on the given type or not.
	 *
	 * @param clazz the type of interest
	 * @param property the JavaBeans property name
	 * @param elementType the element type to check, must be either {@link ElementType#FIELD} or
	 * {@link ElementType#METHOD}.
	 * @return {@code true} if the specified property exists, {@code false} otherwise
	 */
	public static boolean propertyExists(Class<?> clazz, String property, ElementType elementType) {
		if ( ElementType.FIELD.equals( elementType ) ) {
			return getDeclaredField( clazz, property ) != null;
		}
		else {
			String capitalizedPropertyName = capitalize( property );

			Method method = getMethod( clazz, PROPERTY_ACCESSOR_PREFIX_GET + capitalizedPropertyName );
			if ( method != null && method.getReturnType() != void.class ) {
				return true;
			}

			method = getMethod( clazz, PROPERTY_ACCESSOR_PREFIX_IS + capitalizedPropertyName );
			if ( method != null && method.getReturnType() == boolean.class ) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns the JavaBeans property name of the given method if it is a getter method.
	 * <p/>
	 * Getter methods are methods
	 * <ul>
	 * <li>whose name start with "get" and who have a return type but no parameter or</li>
	 * <li>whose name starts with "is" and who have no parameter and return {@code boolean} or</li>
	 * </ul>
	 *
	 * @param method The method for which to get the property name
	 * @return The property name for the given method or {@code null} if the method is not a getter method according to
	 * the JavaBeans standard
	 */
	public static String getPropertyName(Method method) {
		if ( method.getParameterTypes().length == 0 ) {
			String methodName = method.getName();

			// <PropertyType> get<PropertyName>()
			if ( methodName.startsWith( PROPERTY_ACCESSOR_PREFIX_GET ) && method.getReturnType() != void.class ) {
				return Introspector.decapitalize( methodName.substring( 3 ) );
			}
			// boolean is<PropertyName>()
			else if ( methodName.startsWith( PROPERTY_ACCESSOR_PREFIX_IS ) && method.getReturnType() == boolean.class ) {
				return Introspector.decapitalize( methodName.substring( 2 ) );
			}
		}

		return null;
	}

	private static Field getDeclaredField(Class<?> clazz, String fieldName) {
		try {
			Field field = clazz.getDeclaredField( fieldName );
			field.setAccessible( true );
			return field;
		}
		catch (NoSuchFieldException e) {
			return null;
		}
	}

	private static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
		try {
			return clazz.getMethod( methodName, parameterTypes );
		}
		catch (NoSuchMethodException e) {
			return null;
		}
	}

	private static String capitalize(String property) {
		return property.substring( 0, 1 ).toUpperCase() + property.substring( 1 );
	}
}
