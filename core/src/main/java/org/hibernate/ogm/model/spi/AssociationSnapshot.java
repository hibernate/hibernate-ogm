/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.spi;

import org.hibernate.ogm.model.key.spi.RowKey;

/**
 * Represents the association snapshot as loaded by the datastore.
 * <p>
 * Interface implemented by the datastore dialect to avoid data duplication in memory (if possible). Note that this
 * snapshot will not be modified by the Hibernate OGM engine
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface AssociationSnapshot {

	/**
	 * Returns the row with the specified key from this association, if present.
	 */
	Tuple get(RowKey rowKey);

	/**
	 * Whether this snapshot contains the specified key or not.
	 */
	boolean containsKey(RowKey rowKey);

	/**
	 * Returns the number of rows contained in this snapshot.
	 */
	int size();

	/**
	 * Returns an iterable with the rows contained in this snapshot.
	 */
	Iterable<RowKey> getRowKeys();
}
