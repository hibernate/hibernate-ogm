/* 
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

package org.hibernate.ogm.helper.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.ogm.helper.annotation.impl.ColumnFinder;
import org.hibernate.ogm.helper.annotation.impl.IdFinder;
import org.hibernate.ogm.helper.annotation.impl.JoinColumnFinder;

/**
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public class AnnotationFinder {

	private static final String JPA_ANNOTATION_PREFIX = "^@javax.persistence.";
	private static final String EMBEDDABLE = "Embeddable";
	private static final String ENTITY = "Entity";
	private static final String PUNCT = "\\p{Punct}";
	private final Pattern embeddableAnnotationPattern = Pattern.compile( JPA_ANNOTATION_PREFIX + EMBEDDABLE + PUNCT );
	private final Pattern entityAnnotationPattern = Pattern.compile( JPA_ANNOTATION_PREFIX + ENTITY +  PUNCT);
	private final String GET_DECLARED_ANNOTATIONS = "getDeclaredAnnotations";
	private final String GET_TYPE = "getType";
	private final String GET_DECLARED_FIELDS = "getDeclaredFields";
	private final String GET_RETURN_TYPE = "getReturnType";
	private final String GET_DECLARED_METHODS = "getDeclaredMethods";
	private final String GET_NAME = "getName";
	private final Finder columnFinder = new ColumnFinder();
	private final Finder joinColumnFinder = new JoinColumnFinder();
	private final Finder idFinder = new IdFinder();
	
	/**
	 * Checks if a Class is annotated by @Embeddable or not.
	 * @param cls Class to be examined.
	 * @return True if the Class is annotated by @Embeddable, otherwise false.
	 */
	public boolean isEmbeddableAnnotated(Class cls) {

		return cls == null ? false : isAnnotatedBy( cls.getDeclaredAnnotations() , embeddableAnnotationPattern);
	}

	/**
	 * Checks if a Class is annotated by @Entity or not.
	 * @param cls Class to be examined.
	 * @return True if the Class is annotated by @Entity, otherwise false.
	 */
	public boolean isEntityAnnotated(Class cls) {

		return cls == null ? false : isAnnotatedBy( cls.getDeclaredAnnotations(), entityAnnotationPattern );
	}
	
	/**
	 * Checks if Annotation array has a pattern.
	 * @param annotations Annotation array to be examined.
	 * @param pattern Pattern to be checked against Annotation array.
	 * @return True if one of the elements in Annotation array has the pattern, otherwise false.
	 */
	private boolean isAnnotatedBy(Annotation[] annotations, Pattern pattern) {

		for ( Annotation annotation : annotations ) {
			Matcher matcher = pattern.matcher( annotation.toString() );
			while(matcher.find()){
				return true;
			}
		}

		return false;
	}
	
	/**
	 * Finds all @Column annotations on fields and methods from the parameter, cls. The method searches for the
	 * annotation on every field and method. If the parameter, fieldName is specified, then the method only searches @Column
	 * annotation on the corresponding field and method to the name.
	 * 
	 * @param cls
	 *            Where the search starts.
	 * @param fieldName
	 *            Constraint for the search.
	 * @return Mapping information for @Column annotation having column name and the class.
	 */
	public Map<String, Class> findAllColumnNamesFrom(Class cls, String fieldName, boolean fast) {

		if ( isNull( cls ) ) {
			return Collections.EMPTY_MAP;
		}

		Map<String, Class> columnMap = new HashMap<String, Class>();
		findFieldColumns( cls, fieldName, columnMap ,fast);
		findMethodColumns( cls, fieldName, columnMap , fast );
		return columnMap;
	}
	
	public Map<String, Class> findAllJoinColumnNamesFrom(Class cls, String fieldName, boolean fast) {

		if ( isNull( cls ) ) {
			return Collections.EMPTY_MAP;
		}

		Map<String, Class> columnMap = new HashMap<String, Class>();
		findFieldJoinColumns( cls, fieldName, columnMap, fast );
		findMethodJoinColumns( cls, fieldName, columnMap, fast );
		return columnMap;
	}
	
	public Map<String,Class> findAllIdsFrom(Class cls,String fieldName,boolean fast){
		if ( isNull( cls ) ) {
			return Collections.EMPTY_MAP;
		}
		Map<String, Class> columnMap = new HashMap<String, Class>();
		findFieldIds( cls, fieldName, columnMap, fast );
		findMethodIds( cls, fieldName, columnMap, fast );
		return columnMap;
		
	}
	
	/**
	 * 
	 * @param cls
	 * @param fieldName
	 * @param columnMap
	 * @param fast
	 * @return
	 */
	public Map<String,Class> findFieldColumns(Class cls, String fieldName, Map<String, Class> columnMap,
			boolean fast){
		
		return findFieldAnnotations(cls,fieldName,columnMap,
				fast,columnFinder);
	}
	
	/**
	 * 
	 * @param cls
	 * @param fieldName
	 * @param columnMap
	 * @param fast
	 * @return
	 */
	public Map<String, Class> findFieldJoinColumns(Class cls, String fieldName, Map<String, Class> columnMap,
			boolean fast) {
		return findFieldAnnotations( cls, fieldName, columnMap, fast, joinColumnFinder );
	}
	
	public Map<String, Class> findFieldIds(Class cls, String fieldName, Map<String, Class> columnMap, boolean fast) {
		return findFieldAnnotations( cls, fieldName, columnMap, fast, idFinder );
	}
	
	/**
	 * Finds fields equal to the parameter, fieldName, annotated by @Column
	 * 
	 * @param cls
	 *            Class to be examined.
	 * @param fieldName
	 *            Field name to be checked against each field in Class.
	 * @param columnMap
	 *            Store found @Column as map. The key is column name specified in @Column and the value is Class
	 *            represented by the field.
	 * @return columnMap.
	 */
	private Map<String, Class> findFieldAnnotations(Class cls, String fieldName, Map<String, Class> columnMap,
			boolean fast, Finder finder) {

		if ( isNull( cls ) ) {
			return Collections.EMPTY_MAP;
		}

		int size = cls.getDeclaredFields().length;
		List fields = new ArrayList( Arrays.asList( cls.getDeclaredFields() ) );
		String columnName = "";
		if ( fieldName != null && !fieldName.equals( "" ) ) {
			Pattern pattern = Pattern.compile( "^" + fieldName + "$", Pattern.CASE_INSENSITIVE );
			for ( int i = 0; i < fields.size(); i++ ) {
				addTo( i, fields, columnName, columnMap, pattern, GET_TYPE, GET_DECLARED_FIELDS, finder );

				if ( fast && i == ( size - 1 ) ) {
					break;
				}
			}
		}
		else {
			for ( int i = 0; i < fields.size(); i++ ) {
				addTo( i, fields, columnName, columnMap, null, GET_TYPE, GET_DECLARED_FIELDS, finder );

				if ( fast && i == ( size - 1 ) ) {
					break;
				}
			}
		}

		return columnMap;
	}
	
	/**
	 * 
	 * @param cls
	 * @param fieldName
	 * @param columnMap
	 * @param fast
	 * @return
	 */
	public Map<String,Class> findMethodColumns(Class cls, String fieldName, Map<String, Class> columnMap,
			boolean fast){
		
		return findMethodAnnotations(cls,fieldName,columnMap,
				fast,columnFinder);
	}
	
	/**
	 * 
	 * @param cls
	 * @param fieldName
	 * @param columnMap
	 * @param fast
	 * @return
	 */
	public Map<String, Class> findMethodJoinColumns(Class cls, String fieldName, Map<String, Class> columnMap,
			boolean fast) {
		return findMethodAnnotations( cls, fieldName, columnMap, fast, joinColumnFinder );
	}
	
	public Map<String, Class> findMethodIds(Class cls, String fieldName, Map<String, Class> columnMap, boolean fast) {
		return findMethodAnnotations( cls, fieldName, columnMap, fast, idFinder );
	}
	
	/**
	 * Finds methods containing the parameter, fieldName, annotated by @Column
	 * 
	 * @param cls
	 *            Class to be examined.
	 * @param fieldName
	 *            Field name to be checked against each method in Class.
	 * @param columnMap
	 *            Store found @Column as map. The key is column name specified in @Column and the value is Class
	 *            represented by the field.
	 * @return columnMap.
	 */
	private Map<String, Class> findMethodAnnotations(Class cls, String fieldName, Map<String, Class> columnMap,
			boolean fast, Finder finder) {

		if ( isNull( cls ) ) {
			return Collections.EMPTY_MAP;
		}

		int size = cls.getDeclaredMethods().length;
		List methods = new ArrayList( Arrays.asList( cls.getDeclaredMethods() ) );
		String columnName = "";

		if ( fieldName != null && !fieldName.equals( "" ) ) {
			Pattern pattern = Pattern.compile( "^(get|is)\\w*" + fieldName + "$", Pattern.CASE_INSENSITIVE );
			for ( int i = 0; i < methods.size(); i++ ) {
				addTo( i, methods, columnName, columnMap, pattern, GET_RETURN_TYPE, GET_DECLARED_METHODS, finder );

				if ( fast && i == ( size - 1 ) ) {
					break;
				}
			}
		}
		else {
			for ( int i = 0; i < methods.size(); i++ ) {
				addTo( i, methods, columnName, columnMap, null, GET_RETURN_TYPE, GET_DECLARED_METHODS, finder );

				if ( fast && i == ( size - 1 ) ) {
					break;
				}
			}
		}

		return columnMap;
	}

	/**
	 * Checks if the parameter, str has the parameter, pattern.
	 * 
	 * @param str
	 *            String to be examined.
	 * @param pattern
	 *            Used to check the parameter, str.
	 * @return True if patter is included.
	 */
	private boolean hasPattern(String str, Pattern pattern) {

		Matcher matcher = pattern.matcher( str );
		while ( matcher.find() ) {
			return true;
		}

		return false;
	}

	/**
	 * Finds field annotated by @Column in List and adds it to the map, Map<String,Class>.
	 * 
	 * @param index
	 *            Index in the List.
	 * @param list
	 *            Stores Field or Method.
	 * @param columnName
	 *            Used to check against every field or method name if the parameter is specified. If the parameter is
	 *            null or ''
	 *            then,
	 *            all the fields or methods are picked up by the method instead of only fields or methods that have the
	 *            specified name.
	 * @param columnMap
	 *            Store found field data as Map<String,Class>.
	 * @param pattern
	 *            Constraint for the search.
	 * @param typeMethod
	 *            Method to get the next candidate class, Field.getType() or Method.getReturnType().
	 * @param fieldOrMethodGetter
	 *            Method to get the next candidate fields or methods, Class.getDeclaredFields() or
	 *            Class.getDeclaredMethods().
	 */
	private void addTo(int index, List list, String columnName, Map<String, Class> columnMap, Pattern pattern,
			String typeMethod, String fieldOrMethodGetter, Finder finder) {
		// TODO prepare for composite key support
		try {
			Object obj = list.get( index );

			if ( pattern != null
					&& !hasPattern( (String) obj.getClass().getDeclaredMethod( GET_NAME ).invoke( obj ), pattern ) ) {
				columnName = null;
			}
			else if ( pattern == null
					|| ( pattern != null && hasPattern(
							(String) obj.getClass().getDeclaredMethod( GET_NAME ).invoke( obj ), pattern ) ) ) {
				columnName = finder.findAnnotation( (Annotation[]) getInheritedMethod( obj, GET_DECLARED_ANNOTATIONS )
						.invoke( obj ), obj );
			}

			Class cls = (Class) ( obj.getClass().getDeclaredMethod( typeMethod ).invoke( obj ) );
			if ( columnName != null && !columnName.equals( "" ) ) {
				columnMap.put( columnName, cls );
			}

			try {
				list.addAll( Arrays.asList( addIfNotAlreadyExist( list,
						(Object[]) cls.getDeclaredMethod( fieldOrMethodGetter ).invoke( cls ) ) ) );
			}
			catch ( NoSuchMethodException ex ) {
				list.addAll( Arrays.asList( addIfNotAlreadyExist( list,
						(Object[]) cls.getClass().getDeclaredMethod( fieldOrMethodGetter ).invoke( cls ) ) ) );
			}
		}
		catch ( Throwable ex ) {
			throw new RuntimeException( ex );
		}
	}

	/**
	 * Gets inherited method.
	 * 
	 * @param obj
	 *            Object to be examined.
	 * @param methodName
	 *            Method name to be used to narrow.
	 * @return Method whose name equals to the parameter, methodName.
	 */
	private Method getInheritedMethod(Object obj, String methodName) {

		Class cl = obj.getClass();
		Method[] methods = cl.getDeclaredMethods();
		Method method = null;
		int i = 0;
		for ( ; i < methods.length; i++ ) {
			if ( methods[i].getName().equals( methodName ) ) {
				method = methods[i];
				break;
			}
			else {
				if ( i == ( methods.length - 1 ) && method == null ) {
					cl = cl.getSuperclass();
					methods = cl.getDeclaredMethods();
					i = -1;
				}
			}
		}

		return method;
	}

	/**
	 * Checks if a field or method is already contained in fields, the parameter. If there is, the field is not added,
	 * otherwise added and is
	 * examined for @Column annotation.
	 * 
	 * @param list
	 *            List.
	 * @param src
	 *            Object array return by Class.getDeclaredFields() or Class.getDeclaredMethods().
	 * @return Object array containing only unique fields or methods.
	 */
	private Object[] addIfNotAlreadyExist(List list, Object[] src) {

		int duplicates = 0;
		for ( Iterator itr = list.iterator(); itr.hasNext(); ) {
			Object field = itr.next();
			for ( int i = 0; i < src.length; i++ ) {
				if ( field.equals( src[i] ) ) {
					src[i] = null;
					duplicates++;
				}
			}
		}

		Object[] uniqueFields = new Object[src.length - duplicates];
		int index = 0;
		for ( int i = 0; i < src.length; i++ ) {
			if ( src[i] != null ) {
				uniqueFields[index] = src[i];
				index++;
			}
		}
		return uniqueFields;
	}

	/**
	 * Checks if an Object is null or not.
	 * 
	 * @param obj
	 *            Examined if it's null or not.
	 * @return True if it's not null, otherwise false.
	 */
	private boolean isNull(Object obj) {
		return obj == null ? true : false;
	}
}