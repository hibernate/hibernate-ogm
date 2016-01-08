/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.index;

import org.hibernate.ogm.index.OgmIndex;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the <a href="https://docs.mongodb.org/manual/core/index-single//">Single Field Index</a> to be applied to
 * to a single field of the documents in a collection through this {@link Indexed} annotation
 *
 * Note that MongoDB supports single field indexes on fields at the top level of a document and on fields in sub-documents.
 * or by specifying the type of a custom  implementation.
 *
 * @author Francois Le Droff
 */
@Target({ FIELD })
@Retention(RUNTIME)
@OgmIndex
public @interface Indexed {

	IndexOrder order() default IndexOrder.ASCENDING;

	/**
	 * Optional. Builds the index in the background so that building an index does not block other database activities.
	 * @return true to build in the background. The default value is false.
	 */
	boolean background() default false;

	/**
	 * A unique index causes MongoDB to reject all documents that contain a duplicate value for the indexed field.
	 *
	 * @see http://docs.mongodb.org/manual/core/index-unique/
	 * @return true if the indexed field is bound to be unique within the collection
	 */
	boolean unique() default false;

	/**
	 * Optional. The name of the index. If unspecified, MongoDB generates an index name
	 * by concatenating the names of the indexed fields and the sort order.
	 *
	 * Whether user specified or MongoDB generated, index names including their full namespace (i.e. database.collection)
	 * cannot be longer than the Index Name Limit (that is 128 characters)
	 *
	 * @return
	 */
	String name() default "";

	//TODO partialFilterExpression

	/**
	 * Optional. If true, the index only references documents with the specified field.
	 * These indexes use less space but behave differently in some situations (particularly sorts).
	 * The default value is false
	 * @return true if the index only references documents with the specified field
	 */
	boolean sparse() default false;


	/**
	 * Optional. Specifies a value, in seconds, as a TTL to control
	 * how long MongoDB retains documents in this collection
	 * @return the number of seconds after which the collection should expire. Defaults to -1 for no expiry
	 */
	int expireAfterSeconds() default -1;


	//TODO storageEngine


}
