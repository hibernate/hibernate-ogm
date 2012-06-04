package org.hibernate.ogm.helper.annotation.impl;

import java.lang.annotation.Annotation;

public class JoinColumnFinder extends ColumnFinder {

	@Override
	public String findAnnotation(Annotation[] annotations, Object obj) {
		return findAnnotationBy( annotations, "@javax.persistence.JoinColumn(", obj );
	}
}
