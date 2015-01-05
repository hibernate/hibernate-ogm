/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.spi;

import java.util.Set;

/**
 * Represents a tuple snapshot as loaded by the datastore.
 * <p>
 * This interface is to be implemented by dialects to avoid data duplication in memory (if possible), typically wrapping
 * a store-specific representation of the tuple data. This snapshot will never be modified by the Hibernate OGM engine.
 * <p>
 * Note that in the case of embeddables (e.g. composite ids), column names are given using dot notation, e.g.
 * "id.countryCode" or "address.city.zipCode". The column names of the physical JPA model will be used, as e.g. given
 * via {@code @Column} .
 * <p>
 * In some special cases implementations may chose to persist different names than mandated by this model, e.g. always
 * {@code _id} will be used as id column name by MongoDB. It is the responsibility of such implementation in this case
 * to do the required translation of column names internally.
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
