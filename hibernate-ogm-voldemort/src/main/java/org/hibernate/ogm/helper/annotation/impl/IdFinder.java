package org.hibernate.ogm.helper.annotation.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.hibernate.ogm.helper.annotation.AbstractFinder;

public class IdFinder extends AbstractFinder {
	
	private final ColumnFinder columnFinder = new ColumnFinder();

	public String findAnnotation(Annotation[] annotations, Object obj) {
		return findAnnotationBy( annotations, "@javax.persistence.Id(", obj );
	}

	public String findAnnotationBy(Annotation[] annotations, String ann, Object obj) {
		for ( Annotation annotation : annotations ) {
			String annStr = annotation.toString();
			if ( annStr.startsWith( ann ) ) {

				try {
					String name = "";
					if ( obj instanceof Field ) {
						name = columnFinder.findAnnotation(
								(Annotation[]) getInheritedMethod( obj, "getDeclaredAnnotations" ).invoke( obj ), obj );
						return name.equals( "" ) ? (String) obj.getClass().getDeclaredMethod( "getName" ).invoke( obj )
								: name;
					}
					else if ( obj instanceof Method ) {
						name = columnFinder.findAnnotation(
								(Annotation[]) getInheritedMethod( obj, "getDeclaredAnnotations" ).invoke( obj ), obj );
						return name.equals( "" ) ? findFieldNameFor(
								(String) obj.getClass().getDeclaredMethod( "getName" ).invoke( obj ),
								( (Class) obj.getClass().getDeclaredMethod( "getDeclaringClass" ).invoke( obj ) )
										.getDeclaredFields() ) : name;
					}
				}
				catch ( Throwable ex ) {
					throw new RuntimeException( ex );
				}
			}
		}

		return "";
	}

}
