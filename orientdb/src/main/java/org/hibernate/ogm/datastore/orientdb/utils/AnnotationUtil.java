/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.utils;

import java.lang.annotation.Annotation;
import javax.persistence.Entity;

/**
 * Util class for working with annotations
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */

public class AnnotationUtil {

	public static Entity findEntityAnnotation(Class entityClass) {
		Annotation[] annotations = entityClass.getAnnotations();
		Entity entityAnnotation = null;
		for ( Annotation annotation : annotations ) {
			if ( annotation instanceof Entity ) {
				entityAnnotation = (Entity) annotation;
			}
		}
		return entityAnnotation;
	}

}
