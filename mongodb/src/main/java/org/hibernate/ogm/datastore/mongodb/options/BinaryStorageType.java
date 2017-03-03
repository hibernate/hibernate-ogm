/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options;

/**
 * Enumeration with types of binary data storaging
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public enum BinaryStorageType {
	/**
	 * Store a content in GridFS
	 */
	GRID_FS,
	/**
	 * Store content inside document
	 */
	DOCUMENT
}
