/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispan.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Reflection utilities.
 *
 * @author The Viet Nguyen &amp;ntviet18@gmail.com&amp;
 */
public class ReflectionHelper {

	/**
	 * Introspect the given object.
	 *
	 * @param obj object for introspection.
	 *
	 * @return a map containing object's field values.
	 *
	 * @throws IntrospectionException if an exception occurs during introspection
	 * @throws InvocationTargetException if property getter throws an exception
	 * @throws IllegalAccessException if property getter is inaccessible
	 */
	public static Map<String, Object> introspect(Object obj)
			throws IntrospectionException, InvocationTargetException, IllegalAccessException {
		Map<String, Object> result = new HashMap<>();
		BeanInfo info = Introspector.getBeanInfo( obj.getClass() );
		for ( PropertyDescriptor pd : info.getPropertyDescriptors() ) {
			Method reader = pd.getReadMethod();
			String name = pd.getName();
			if ( reader != null && !"class".equals( name ) ) {
				result.put( name, reader.invoke( obj ) );
			}
		}
		return result;
	}

	/**
	 * Check if a given type is primitive reference. Primitive references are Java primitives, their wrapper classes,
	 * Void and String types.
	 *
	 * @param refType type for checking
	 *
	 * @return true if the given type is primitive reference.
	 */
	public static boolean isPrimitiveRef(Class<?> refType) {
		return refType.isPrimitive() || refType == String.class || refType == Boolean.class ||
				refType == Byte.class || refType == Character.class || refType == Double.class ||
				refType == Float.class || refType == Integer.class || refType == Long.class ||
				refType == Short.class || refType == Void.class;
	}

	/**
	 * Load and instantiate given callable type.
	 *
	 * @param className full qualified class name
	 *
	 * @return new object of given class.
	 *
	 * @throws ClassNotFoundException if the class cannot be located
	 * @throws IllegalAccessException if class or its no-arg constructor are not accessible
	 * @throws InstantiationException if the class cannot be instantiated, e.g. abstract class, interface, etc.
	 */
	public static Callable<?> instantiate(String className)
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		Class<?> clazz = Class.forName( className );
		return (Callable<?>) clazz.newInstance();
	}

	/**
	 * Set value for given object field.
	 *
	 * @param object object to be updated
	 * @param field field name
	 * @param value field value
	 *
	 * @throws NoSuchMethodException if property writer is not available
	 * @throws InvocationTargetException if property writer throws an exception
	 * @throws IllegalAccessException if property writer is inaccessible
	 */
	private static void setField(Object object, String field, Object value)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Class<?> clazz = object.getClass();
		Method m = clazz.getMethod( "set" + capitalize( field ), value.getClass() );
		m.invoke( object, value );
	}

	/**
	 * Set values for given object by property value map.
	 *
	 * @param object object to be updated
	 * @param params property value map
	 *
	 * @throws NoSuchMethodException if property writer is not available
	 * @throws InvocationTargetException if property writer throws an exception
	 * @throws IllegalAccessException if property writer is inaccessible
	 */
	public static void setFields(Object object, Map<String, Object> params)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		for ( Map.Entry<String, Object> entry : params.entrySet() ) {
			setField( object, entry.getKey(), entry.getValue() );
		}
	}

	private static String capitalize(String str) {
		return str.substring( 0, 1 ).toUpperCase() + str.substring( 1 );
	}
}
