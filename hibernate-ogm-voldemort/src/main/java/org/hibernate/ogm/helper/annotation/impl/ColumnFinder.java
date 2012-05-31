package org.hibernate.ogm.helper.annotation.impl;

import java.lang.annotation.Annotation;
import java.util.regex.Pattern;

import org.hibernate.ogm.helper.annotation.AbstractFinder;

public class ColumnFinder extends AbstractFinder {

	private final Pattern namePropertyPattern = Pattern.compile( "name\\p{Punct}\\w*" );
	
	/**
	 * Finds column name from @Column annotation in Annotation array.
	 * 
	 * @param annotations
	 *            Annotation array to be examined.
	 * @return Column name from @Column annotation.
	 */
	public String findAnnotation(Annotation[] annotations, Object obj) {
		return findAnnotationBy( annotations, "@javax.persistence.Column(", obj );
	}

	public String findAnnotationBy(Annotation[] annotations, String ann, Object obj) {
		for ( Annotation annotation : annotations ) {
			String annStr = annotation.toString();
			if ( annStr.startsWith( ann ) ) {
				return extractColumnNameFrom( annStr, namePropertyPattern );
			}
		}

		return "";
	}

}
