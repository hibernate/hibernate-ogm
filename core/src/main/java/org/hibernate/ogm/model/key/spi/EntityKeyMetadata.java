/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.key.spi;


/**
 * Stores metadata information common to all keys related to a given entity.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface EntityKeyMetadata {

	/**
	 * Get the name of the table representing the entity
	 *
	 * @return the entity table name
	 */
	String getTable();

	/**
	 * This class should be treated as immutable. While we expose this array, you should never make changes to it! This
	 * is a design tradeoff vs. raw performance and memory usage.
	 *
	 * @return the name of the columns
	 */
	String[] getColumnNames();

	/**
	 * Whether the given column is part of this key family or not.
	 *
	 * @param columnName the column to check
	 * @return {@code true} if the given column is part of this key, {@code false} otherwise.
	 */
	boolean isKeyColumn(String columnName);
}
