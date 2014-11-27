/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.spi;

import java.util.Set;

/**
 * Represents the Tuple snapshot as loaded by the datastore.
 * Interface implemented by the datastore dialect to avoid data
 * duplication in memory (if possible).
 *
 * Note that this snapshot will not be modified by the Hibernate OGM engine
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface TupleSnapshot {
	/**
	 * Get the value of a column in the tuple
	 *
	 * @param column the name of the column
	 * @return the column value or {@code null} if the value is not set
	 */
	Object get(String column);

	/**
	 * Check if the tuple contains some values
	 *
	 * @return {@code true} is there is at lease one value in the tuple, {@code false} otherwise.
	 */
	boolean isEmpty();

	/**
	 * Get columns names composing the tuple
	 *
	 * @return the columns names
	 */
	Set<String> getColumnNames();
}
