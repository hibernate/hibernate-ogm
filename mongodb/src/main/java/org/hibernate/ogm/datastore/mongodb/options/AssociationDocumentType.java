/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options;

/**
 * Strategies for storing MongoDB association documents.
 *
 * @author Gunnar Morling
 */
public enum AssociationDocumentType {

	/**
	 * Stores the association info in the same MongoDB collection for all associations
	 */
	GLOBAL_COLLECTION,

	/**
	 * Stores the association in a dedicated MongoDB collection per association
	 */
	COLLECTION_PER_ASSOCIATION
}
