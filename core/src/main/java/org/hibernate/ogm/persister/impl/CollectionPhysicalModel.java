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
	 */
	String getTableName();

	/**
	 * The columns to join on
	 */
	String[] getKeyColumnNames();

	/**
	 * Get the names of the collection index columns if
	 * this is an indexed collection (optional operation)
	 */
	String[] getIndexColumnNames();

	/**
	 * Get the names of the collection element columns (or the primary
	 * key columns in the case of a one-to-many association)
	 */
	String[] getElementColumnNames();

	String getIdentifierColumnName();

	//The following should really be moved somewhere else or the interface renamed
	GridType getKeyGridType();

	GridType getElementGridType();
}
