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
	 * Get the row associated to the {@link RowKey}.
	 *
	 * @param rowKey the identifier of the row in the association
	 * @return the row with the specified key from this association, if present. {@code null} otherwise.
	 */
	Tuple get(RowKey rowKey);

	/**
	 * Whether this snapshot contains the specified key or not.
	 *
	 * @param rowKey the identifier of the row in the association
	 * @return {@code true} if the snapshot contains the row identified by the {@link RowKey}. {@code false} otherwise
	 */
	boolean containsKey(RowKey rowKey);

	/**
	 * Returns the number of rows contained in this snapshot.
	 *
	 * @return the number of rows in the association snapshot
	 */
	int size();

	/**
	 * Returns an iterable with the rows contained in this snapshot.
	 *
	 * @return an {@link Iterable} over the rows in the association
	 */
	Iterable<RowKey> getRowKeys();
}
