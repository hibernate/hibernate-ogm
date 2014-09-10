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
public class IdSourceKeyMetadata {

	/**
	 * The type of an id sequence source.
	 */
	public enum IdSourceType {
		TABLE, SEQUENCE;
	}

	private final IdSourceType type;
	private final String name;
	private final String keyColumnName;
	private final String valueColumnName;
	private final int hashCode;

	private IdSourceKeyMetadata(IdSourceType type, String name, String keyColumnName, String valueColumnName) {
		this.type = type;
		this.name = name;
		this.keyColumnName = keyColumnName;
		this.valueColumnName = valueColumnName;
		this.hashCode = calculateHashCode();
	}

	public static IdSourceKeyMetadata forTable(String table, String keyColumnName, String valueColumnName) {
		return new IdSourceKeyMetadata( IdSourceType.TABLE, table, keyColumnName, valueColumnName );
	}

	public static IdSourceKeyMetadata forSequence(String sequence) {
		return new IdSourceKeyMetadata( IdSourceType.SEQUENCE, sequence, null, null );
	}

	/**
	 * Returns the type of the represented id source.
	 */
	public IdSourceType getType() {
		return type;
	}

	/**
	 * Returns the table name for table-based sources, the sequence name otherwise.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the key column name for table-based sources, {@code null} otherwise.
	 */
	public String getKeyColumnName() {
		return keyColumnName;
	}

	/**
	 * Returns the value column name for table-based sources, {@code null} otherwise.
	 */
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
		IdSourceKeyMetadata other = (IdSourceKeyMetadata) obj;
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
		return "IdSourceKeyMetadata [type=" + type + ", name=" + name + ", keyColumnName=" + keyColumnName + ", valueColumnName=" + valueColumnName + "]";
	}

	private int calculateHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
		result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
		return result;
	}
}
