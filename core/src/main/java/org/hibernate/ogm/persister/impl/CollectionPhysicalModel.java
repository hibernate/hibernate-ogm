/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Emmanuel Bernard
 */
public interface CollectionPhysicalModel extends CollectionPersister {
	/**
	 * The table to join to.
	 *
	 * @return the names of the table to join to
	 */
	String getTableName();

	/**
	 * The columns to join on
	 *
	 * @return the columns names representing the key
	 */
	String[] getKeyColumnNames();

	/**
	 * Get the names of the collection index columns if
	 * this is an indexed collection (optional operation)
	 *
	 * @return the columns names representing the index
	 */
	String[] getIndexColumnNames();

	/**
	 * Get the names of the collection element columns (or the primary
	 * key columns in the case of a one-to-many association)
	 *
	 * @return the columns names representing the element
	 */
	String[] getElementColumnNames();

	/**
	 * Get the name of the column identifier.
	 *
	 * @return the column representing the identifier
	 */
	String getIdentifierColumnName();

	//The following should really be moved somewhere else or the interface renamed
	GridType getKeyGridType();

	GridType getElementGridType();
}
