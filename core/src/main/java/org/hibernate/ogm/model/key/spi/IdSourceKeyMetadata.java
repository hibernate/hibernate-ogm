/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.key.spi;

/**
 * Meta-data about an id sequence source. In the case of table-based generators, corresponds to the table used by one
 * ore more generators. In the case of sequence-based generators, corresponds to one sequence.
 *
 * @author Gunnar Morling
 */
public interface IdSourceKeyMetadata {

	/**
	 * The type of an id sequence source.
	 */
	public enum IdSourceType {
		TABLE, SEQUENCE;
	}

	/**
	 * Get the type of the represented id source.
	 *
	 * @return type of the id source
	 */
	IdSourceType getType();

	/**
	 * Returns the table name for table-based sources, the sequence name otherwise.
	 *
	 * @return the table name for table-based sources, the sequence name otherwise
	 */
	String getName();

	/**
	 * Get the key column name for table-based sources, {@code null} otherwise.
	 *
	 * @return the key column name for table-based sources, {@code null} otherwise.
	 */
	String getKeyColumnName();

	/**
	 * Get the value column name for table-based sources, {@code null} otherwise.
	 *
	 * @return the value column name for table-based sources, {@code null} otherwise.
	 */
	String getValueColumnName();
}
