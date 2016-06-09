/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.ogm.datastore.mongodb.options.impl.MongoDBCollectionConverter;
import org.hibernate.ogm.options.spi.MappingOption;

/**
 * MongoDB specific definitions for a collection.
 *
 * @author Guillaume Smet
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@MappingOption(MongoDBCollectionConverter.class)
public @interface MongoDBCollection {

	MongoDBIndexOptions[] indexOptions() default {};

}
