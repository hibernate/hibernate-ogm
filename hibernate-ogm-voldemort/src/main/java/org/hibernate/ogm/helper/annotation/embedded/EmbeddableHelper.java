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

package org.hibernate.ogm.helper.annotation.embedded;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * @author Seiya Kawashima <skawashima@uchicago.edu>
 */
public class EmbeddableHelper {

	private static final Log log = LoggerFactory.make();

	public EmbeddableObject getObjectFromEmbeddableOn(String columnName, Map<String, Object> tuple, Class cls) {

		log.info( "col name: " + findColumnNameFromFieldAnnotationOn( cls ) );
		return matchTupleOn( findAllEmbeddableColumnsFrom( findFieldNameFrom( columnName ), tuple ), cls );
	}

	/**
	 * Finds embeddable field name.
	 * 
	 * @param columnName
	 *            Column name used to find field name.
	 * @return Field name.
	 */
	private String findFieldNameFrom(String columnName) {
		if ( !columnName.contains( "." ) ) {
			throw new RuntimeException(
					"Embeddable column name must contain '.' separating field name and the property but " + columnName );

		}

		log.info( "field name for embeddable: " + columnName.substring( 0, columnName.lastIndexOf( "." ) ) );
		return columnName.substring( 0, columnName.lastIndexOf( "." ) );
	}

	/**
	 * Finds all embeddable columns from a tuple.
	 * 
	 * @param embeddedFieldName
	 *            Field name used to find all the embeddable columns.
	 * @param tuple
	 *            Tuple where all the embeddable columns are found.
	 * @return All the embeddable column names.
	 */
	private Map<String, Object> findAllEmbeddableColumnsFrom(String embeddedFieldName, Map<String, Object> tuple) {

		Map<String, Object> columns = new HashMap<String, Object>();
		for ( Iterator<String> itr = tuple.keySet().iterator(); itr.hasNext(); ) {
			String key = itr.next();
			if ( key.startsWith( embeddedFieldName ) ) {
				log.info( "found embeddable column name: " + key + " value: " + tuple.get( key ) );
				columns.put( key, tuple.get( key ) );
			}
		}

		log.info( "all embeddable columns: " + columns );
		return columns;
	}

	private EmbeddableObject matchTupleOn(Map<String, Object> tuple, Class cls) {

		Map<String, Class> classes = new HashMap<String, Class>();
		for ( Field f : cls.getDeclaredFields() ) {
			Field field = f;
			log.info( "all field: " + field );
			for ( Iterator<String> itr = tuple.keySet().iterator(); itr.hasNext(); ) {
				String key = itr.next();
				log.info( "key:L " + key );
				if ( key.contains( "." ) ) {
					String[] keyParts = key.split( "\\." );
					EmbeddedObject embeddedObject = getEmbeddedFieldFrom( keyParts[keyParts.length - 1], cls );
					if ( embeddedObject.getField() == null ) {
						addEmbeddedFieldOn( classes, embeddedObject.getField(), cls, keyParts[keyParts.length - 1], key );
					}
					else {
						addEmbeddedFieldOn( classes, embeddedObject.getField(), embeddedObject.getCls(),
								keyParts[keyParts.length - 1], key );
					}
				}
				else {
					addEmbeddedFieldOn( classes, field, cls, key, null );
				}
			}
		}

		log.info( "classes: " + classes );
		return new EmbeddableObject( tuple, classes );
	}

	private EmbeddedObject getEmbeddedFieldFrom(String key, Class cls) {

		Field embeddedField = null;
		Class embeddedClass = null;
		Class originalClass = cls;

		for ( Field field : cls.getDeclaredFields() ) {
			if ( field.getName().equals( key ) ) {
				embeddedField = field;
				break;
			}
		}

		for ( Field field : cls.getDeclaredFields() ) {
			Field f = null;
			for ( Field fld : field.getType().getDeclaredFields() ) {
				f = fld;
				if ( f.getName().equals( key ) ) {
					embeddedField = f;
					embeddedClass = f.getDeclaringClass();
					break;
				}
			}
			cls = f.getType();
		}

		if ( embeddedField == null ) {
			cls = originalClass;
			for ( Field field : cls.getDeclaredFields() ) {
				Field f = null;
				for ( Field fld : field.getType().getDeclaredFields() ) {
					f = fld;
					if ( !getMatchedColumnAnnotationOn( f ).equals( "" ) ) {
						embeddedField = f;
						embeddedClass = f.getDeclaringClass();
						break;
					}
				}
				cls = f.getType();
			}
		}

		log.info( "found embedded field is: " + embeddedField + " key: " + key + " cls: " + cls );
		return new EmbeddedObject( embeddedField, embeddedClass );
	}

	private void addEmbeddedFieldOn(Map<String, Class> classes, Field field, Class cls, String key, String originalKey) {

		if ( field == null ) {
			Class returnType = findReturnTypeFromColumnAnootationOn( cls );
			log.info( "@Column from method return type: " + returnType );
			if ( returnType != null ) {
				classes.put( key, returnType );
			}
			else {
				throw new RuntimeException( "could not find field and method return type for key: " + key );
			}
			return;
		}

		if ( field.getName().equals( key ) ) {
			log.info( "match tuple with field: field name: " + field.getName() + " type: " + field.getType() );

			if ( originalKey != null && !originalKey.equals( "" ) ) {
				classes.put( originalKey, field.getType() );
			}
			else {
				classes.put( field.getName(), field.getType() );
			}
			return;
		}

		String foundColumnName = getMatchedColumnAnnotationOn( field );
		if ( !foundColumnName.equals( "" ) ) {
			log.info( "match tuple with Column annotation: column name " + foundColumnName + " type: "
					+ field.getType() );
			if ( originalKey != null && !originalKey.equals( "" ) ) {
				classes.put( originalKey, field.getType() );
			}
			else {
				classes.put( field.getName(), field.getType() );
			}
			return;
		}

		foundColumnName = getMatchedColumnAnnotationOn( field.getName(), cls );
		if ( !foundColumnName.equals( "" ) ) {
			log.info( "match tuple with method Column annotation: column name " + foundColumnName + " type: "
					+ field.getType() );
			if ( originalKey != null && !originalKey.equals( "" ) ) {
				classes.put( originalKey, field.getType() );
			}
			else {
				classes.put( field.getName(), field.getType() );
			}
			return;
		}
	}

	private String getMatchedColumnAnnotationOn(Field field) {

		return findColumnAnnotation( field.getDeclaredAnnotations() );
	}

	private String getMatchedColumnAnnotationOn(String fieldName, Class cls) {

		Pattern pattern = Pattern.compile( "^get\\w*" + fieldName + "$", Pattern.CASE_INSENSITIVE );
		for ( Method method : cls.getDeclaredMethods() ) {

			Matcher matcher = pattern.matcher( method.getName() );
			while ( matcher.find() ) {
				return findColumnAnnotation( method.getDeclaredAnnotations() );
			}
		}

		return "";
	}

	private String findColumnAnnotation(Annotation[] annotations) {

		Pattern pattern = Pattern.compile( "name\\p{Punct}\\w*" );
		for ( Annotation annotation : annotations ) {
			String annStr = annotation.toString();
			if ( annStr.contains( "javax.persistence.Column" ) ) {
				log.info( "annotation contains 'Column' annotation: " + annStr );
				return extractColumnNameFrom( annStr, pattern );
			}
		}

		return "";
	}

	private String extractColumnNameFrom(String annotationStr, Pattern pattern) {

		Matcher matcher = pattern.matcher( annotationStr );
		String columnName = "";
		while ( matcher.find() ) {
			columnName = matcher.group().split( "=" )[1];
			log.info( "column name from matcher: " + columnName );
		}

		return columnName;
	}

	private Class findReturnTypeFromColumnAnootationOn(Class cls) {

		Class returnType = null;
		for ( Method method : cls.getDeclaredMethods() ) {
			if ( !findColumnAnnotation( method.getDeclaredAnnotations() ).equals( "" ) ) {
				log.info( "found @Column on " + method );
				returnType = method.getReturnType();
				break;
			}
		}

		return returnType;
	}

	private String findColumnNameFromFieldAnnotationOn(Class cls) {

		String s = "";
		for ( Field field : cls.getDeclaredFields() ) {
			log.info( "field name: " + field );
			String columnName = getMatchedColumnAnnotationOn( field );
			if ( !columnName.equals( "" ) ) {
				s = columnName;
				break;
			}

			s = findColumnNameFromFieldAnnotationOn( field.getType() );
			log.info( "s: " + s );
		}

		return s;
	}
}
