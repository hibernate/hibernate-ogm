/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.options;

/**
 * Strategies for storing the keys and values of map-typed associations.
 *
 * @author Gunnar Morling
 */
public enum MapStorageType {

	/**
	 * For map-typed associations with a single key column which is of type {@code String}, the following document
	 * representation will be persisted:
	 *
	 * <pre>
	 * ...
	 * "addresses" : {
	 *     "home" : 123,
	 *     "work" : 456
	 * }
	 * ...
	 * </pre>
	 *
	 * This setting is ignored for all other key column types,
	 * {@link MapStorageType#AS_LIST} will be used then.
	 */
	BY_KEY,

	/**
	 * The entries of a map-typed association will be stored as an array with a sub-document for each map entry. All key
	 * and value columns will be contained within the array elements:
	 *
	 * <pre>
	 * ...
	 * "addresses" : [
	 *     { "addressType" : "home", "addressId" : 123 },
	 *     { "addressType" : "work", "addressId" : 456 },
	 * ]
	 * ...
	 * </pre>
	 */
	AS_LIST;
}
