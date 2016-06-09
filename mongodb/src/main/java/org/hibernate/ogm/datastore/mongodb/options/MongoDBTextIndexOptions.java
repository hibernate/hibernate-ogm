/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options;


/**
 * Options specific to MongoDB full text indexes.
 *
 * See <a href="https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/#options-for-text-indexes">https://docs.mongodb.com/manual/reference/method/db.collection.createIndex/#options-for-text-indexes</a>
 *
 * @author Guillaume Smet
 */
public @interface MongoDBTextIndexOptions {

	/**
	 * The relative weights of the fields indexed in the full text index.
	 *
	 * Must be a valid MongoDB document.
	 */
	String weights() default "";

	/**
	 * The default language used to index the field.
	 */
	String defaultLanguage() default "";

	/**
	 * Name of a field used to specify the language on a per document basis. By default, 'language'.
	 */
	String languageOverride() default "";

}
