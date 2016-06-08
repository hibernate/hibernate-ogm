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

/**
 * MongoDB specific options for a given index.
 *
 * See <a href="https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/#options-for-all-index-types">https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/#options-for-all-index-types</a>
 *
 * @author Guillaume Smet
 */
@Target( {TYPE, METHOD, FIELD } )
@Retention(RUNTIME)
public @interface MongoDBIndexOptions {

	/**
	 * Name of the index to which these options apply.
	 */
	String forIndex();

	/**
	 * Builds the index in the background so that building an index does not block other database activities.
	 */
	boolean background() default false;

	/**
	 * Allows to define a partial index by providing a filter expression.
	 *
	 * Must be a valid MongoDB document.
	 */
	String partialFilterExpression() default "";

	/**
	 * If true, the index only references documents with the specified field.
	 * These indexes use less space but behave differently in some situations (particularly sorts).
	 * The default value is false
	 */
	boolean sparse() default false;

	/**
	 * Specifies a value, in seconds, as a TTL to control
	 * how long MongoDB retains documents in this collection
	 */
	int expireAfterSeconds() default -1;

	/**
	 * Allows to define options for the storage engine.
	 *
	 * Must be a valid MongoDB document.
	 */
	String storageEngine() default "";

	/**
	 * Specific options for full text indexes.
	 *
	 * Note that MongoDB only supports one full text index per collection.
	 */
	MongoDBTextIndexOptions[] text() default {};

}
