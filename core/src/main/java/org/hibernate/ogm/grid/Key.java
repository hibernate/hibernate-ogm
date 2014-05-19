/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.grid;

/**
 * The key of an entity, association or row.
 * <p>
 * Note that exposing the state of instances through arrays is a deliberate decision for the sake of performance. The
 * contents of these arrays must not be modified by the caller.
 *
 * @author Gunnar Morling
 */
public interface Key {

	/**
	 * Returns the table name of this key.
	 */
	String getTable();

	/**
	 * Returns the column names of this key.
	 */
	String[] getColumnNames();

	/**
	 * Returns the column values of this key.
	 */
	Object[] getColumnValues();
}
