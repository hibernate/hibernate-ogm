package org.hibernate.ogm.helper.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractFinder implements Finder {
	/**
	 * Extracts column name from @Column(name='xxx').
	 * 
	 * @param annotationStr
	 *            @Column representation as String.
	 * @param pattern
	 *            Pattern for the name property in @Column annotation.
	 * @return Column name.
	 */
	protected String extractColumnNameFrom(String annotationStr, Pattern pattern) {

		Matcher matcher = pattern.matcher( annotationStr );
		String columnName = "";
		while ( matcher.find() ) {
			columnName = matcher.group().split( "=" )[1];
		}

		return columnName;
	}
	
	protected String findFieldNameFor(String methodName, Field[] fields) {
		
		Pattern pattern = null;
		if(methodName.startsWith( "get" )){
			pattern = Pattern.compile( "^" + methodName.substring( 3 ) + "$", Pattern.CASE_INSENSITIVE );
		}else if(methodName.startsWith( "is" )){
			pattern = Pattern.compile( "^" + methodName.substring( 2 ) + "$", Pattern.CASE_INSENSITIVE );
		}
		//Pattern pattern = Pattern.compile( "^" + methodName.substring( 3 ) + "$", Pattern.CASE_INSENSITIVE );
		for ( Field field : fields ) {
			Matcher matcher = pattern.matcher( field.getName() );
			while ( matcher.find() ) {
				return matcher.group();
			}
		}

		return "";
	}
	
	/**
	 * Copied from AnnotationFinder. Gets inherited method.
	 * 
	 * @param obj
	 *            Object to be examined.
	 * @param methodName
	 *            Method name to be used to narrow.
	 * @return Method whose name equals to the parameter, methodName.
	 */
	protected Method getInheritedMethod(Object obj, String methodName) {

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
}
