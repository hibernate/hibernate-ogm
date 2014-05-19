/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import java.util.Set;

import org.hibernate.ogm.grid.RowKey;

/**
 * Represents the Association snapshot as loaded by the datastore.
 * Interface implemented by the datastore dialect to avoid data
 * duplication in memory (if possible).
 *
 * Note that this snapshot will not be modified by the Hibernate OGM engine
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface AssociationSnapshot {

	/**
	 * Returns the value set in a column or null if not set
	 */
	Tuple get(RowKey column);

	boolean containsKey(RowKey column);

	int size();

	Set<RowKey> getRowKeys();
}
