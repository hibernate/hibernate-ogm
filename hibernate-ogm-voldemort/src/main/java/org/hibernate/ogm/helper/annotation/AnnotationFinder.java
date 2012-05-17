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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public class AnnotationFinder {

	private static final Log log = LoggerFactory.make();
	private final Pattern embeddableAnnotationPattern = Pattern.compile( "^\\@javax.persistence.Embeddable\\p{Punct}" );
	private final Pattern namePropertyPattern = Pattern.compile( "name\\p{Punct}\\w*" );

	public boolean isEmbeddableAnnotated(Class cls) {

		return cls == null ? false : isAnnotatedBy( cls.getDeclaredAnnotations() , embeddableAnnotationPattern);
	}

	private boolean isAnnotatedBy(Annotation[] annotations, Pattern pattern) {

		for ( Annotation annotation : annotations ) {
			Matcher matcher = pattern.matcher( annotation.toString() );
			while(matcher.find()){
				log.info("found " + matcher.group());
				return true;
			}
		}

		return false;
	}

	public Map<String, Class> findColumnNameFromFieldOnRecursively(Class cls, Map<String, Class> columnMap) {

		if ( isNull( cls ) ) {
			return Collections.EMPTY_MAP;
		}

		for ( Field field : cls.getDeclaredFields() ) {
			String columnName = findColumnAnnotation( field.getDeclaredAnnotations() );
			if ( !columnName.equals( "" ) ) {
				columnMap.put( columnName, field.getType() );
			}
			else {
				findColumnNameFromFieldOnRecursively( field.getType(), columnMap );
			}
		}

		return columnMap;
	}

	public Map<String,Class> findMethodColumnAnnotationsFrom(Class cls, String fieldName, Map<String,Class> columnMap){
		
		if(fieldName != null && !fieldName.equals("")){
			Pattern pattern = Pattern.compile( "^get\\w*" + fieldName + "$", Pattern.CASE_INSENSITIVE );
			return findColumnNameFromMethodOnRecursively(cls, pattern, columnMap);
		}
		
		return findColumnNameFromMethodOnRecursively( cls, columnMap );
	}
	
	private Map<String, Class> findColumnNameFromMethodOnRecursively(Class cls, Map<String, Class> columnMap) {

		if ( isNull( cls ) ) {
			return Collections.EMPTY_MAP;
		}

		columnMap.putAll( findColumnNameFromMethodOn( cls ) );

		for ( Field field : cls.getDeclaredFields() ) {
			for ( Method method : field.getType().getDeclaredMethods() ) {
				String columnName = findColumnAnnotation( method.getDeclaredAnnotations() );
				if ( !columnName.equals( "" ) ) {
					columnMap.put( columnName, method.getReturnType() );
				}
				else {
					findColumnNameFromMethodOnRecursively( method.getDeclaringClass(), columnMap );
				}
			}
		}

		return columnMap;
	}

	private Map<String, Class> findColumnNameFromMethodOnRecursively(Class cls, Pattern pattern, Map<String, Class> columnMap) {

		if ( isNull( cls ) ) {
			return Collections.EMPTY_MAP;
		}

		columnMap.putAll( findColumnNameFromMethodOn( cls, pattern ) );
		
		for ( Field field : cls.getDeclaredFields() ) {
			for ( Method method : field.getType().getDeclaredMethods() ) {
				if ( hasPattern( method.getName(), pattern ) ) {
					String columnName = findColumnAnnotation( method.getDeclaredAnnotations() );
					if ( !columnName.equals( "" ) ) {
						columnMap.put( columnName, method.getReturnType() );
					}
				}
				else {
					findColumnNameFromMethodOnRecursively( method.getDeclaringClass(), pattern, columnMap );
				}
			}
		}

		return columnMap;
	}
	
	private boolean hasPattern(String str, Pattern pattern){
		
		Matcher matcher = pattern.matcher( str );
		while(matcher.find()){
			return true;
		}
		
		return false;
	}
	
	public Map<String, Class> findColumnNameFromMethodOn(Class cls) {

		if ( isNull( cls ) ) {
			return Collections.EMPTY_MAP;
		}

		Map<String, Class> columnMap = new HashMap<String, Class>();
		for ( Method method : cls.getDeclaredMethods() ) {
			String columnName = findColumnAnnotation( method.getDeclaredAnnotations() );
			if ( !columnName.equals( "" ) ) {
				columnMap.put( columnName, method.getReturnType() );
			}
		}

		return columnMap;
	}
	
	private Map<String, Class> findColumnNameFromMethodOn(Class cls, Pattern pattern) {

		if ( isNull( cls ) ) {
			return Collections.EMPTY_MAP;
		}

		Map<String, Class> columnMap = new HashMap<String, Class>();
		for ( Method method : cls.getDeclaredMethods() ) {
			if ( hasPattern( method.getName(), pattern ) ) {
				String columnName = findColumnAnnotation( method.getDeclaredAnnotations() );
				if(!columnName.equals( "" )){
					columnMap.put( columnName, method.getReturnType() );
				}
			}
		}
		
		return columnMap;
	}

	public Map<String, Class> findAllColumnNamesFrom(Class cls, String fieldName) {

		Map<String, Class> columnMap = new HashMap<String, Class>();
		columnMap = findColumnNameFromFieldOnRecursively( cls, columnMap );
		columnMap.putAll(findMethodColumnAnnotationsFrom(cls, fieldName, columnMap));
		return columnMap;
	}

	private String findColumnAnnotation(Annotation[] annotations) {

		for ( Annotation annotation : annotations ) {
			String annStr = annotation.toString();
			if ( annStr.startsWith( "@javax.persistence.Column(" ) ) {
				return extractColumnNameFrom( annStr, namePropertyPattern );
			}
		}

		return "";
	}

	private String extractColumnNameFrom(String annotationStr, Pattern pattern) {

		Matcher matcher = pattern.matcher( annotationStr );
		String columnName = "";
		while ( matcher.find() ) {
			columnName = matcher.group().split( "=" )[1];
		}

		return columnName;
	}

	private boolean isNull(Object obj) {
		return obj == null ? true : false;
	}

}
