/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * <p>
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
