/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.impl;

import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;

/**
 * Meta-data about an id sequence source. In the case of table-based generators, corresponds to the table used by one
 * ore more generators. In the case of sequence-based generators, corresponds to one sequence.
 *
 * @author Gunnar Morling
 */
public class DefaultIdSourceKeyMetadata implements IdSourceKeyMetadata {

	private final IdSourceType type;
	private final String name;
	private final String keyColumnName;
	private final String valueColumnName;
	private final int hashCode;

	private DefaultIdSourceKeyMetadata(IdSourceType type, String name, String keyColumnName, String valueColumnName) {
		this.type = type;
		this.name = name;
		this.keyColumnName = keyColumnName;
		this.valueColumnName = valueColumnName;
		this.hashCode = calculateHashCode();
	}

	public static DefaultIdSourceKeyMetadata forTable(String table, String keyColumnName, String valueColumnName) {
		return new DefaultIdSourceKeyMetadata( IdSourceType.TABLE, table, keyColumnName, valueColumnName );
	}

	public static DefaultIdSourceKeyMetadata forSequence(String sequence) {
		return new DefaultIdSourceKeyMetadata( IdSourceType.SEQUENCE, sequence, null, null );
	}

	/**
	 * Returns the type of the represented id source.
	 */
	@Override
	public IdSourceType getType() {
		return type;
	}

	/**
	 * Returns the table name for table-based sources, the sequence name otherwise.
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Returns the key column name for table-based sources, {@code null} otherwise.
	 */
	@Override
	public String getKeyColumnName() {
		return keyColumnName;
	}

	/**
	 * Returns the value column name for table-based sources, {@code null} otherwise.
	 */
	@Override
	public String getValueColumnName() {
		return valueColumnName;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		DefaultIdSourceKeyMetadata other = (DefaultIdSourceKeyMetadata) obj;
		if ( name == null ) {
			if ( other.name != null ) {
				return false;
			}
		}
		else if ( !name.equals( other.name ) ) {
			return false;
		}
		if ( type != other.type ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "DefaultIdSourceKeyMetadata [type=" + type + ", name=" + name + ", keyColumnName=" + keyColumnName + ", valueColumnName=" + valueColumnName + "]";
	}

	private int calculateHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
		result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
		return result;
	}
}
