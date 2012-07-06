package org.hibernate.ogm.helper.annotation;

import java.lang.annotation.Annotation;

public interface Finder {
	public String findAnnotation(Annotation[] annotations, Object obj);

	public String findAnnotationBy(Annotation[] annotations, String ann, Object obj);
}
