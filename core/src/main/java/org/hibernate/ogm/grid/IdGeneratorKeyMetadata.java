/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.grid;

/**
 * Meta-data about an id generator. In the case of table-based generators, corresponds to the table used by several
 * generators; In the case of sequence-based generators, corresponds to one sequence.
 *
 * @author Gunnar Morling
 */
public class IdGeneratorKeyMetadata {

	/**
	 * The type of an id generator.
	 */
	public enum IdGeneratorType {
		TABLE, SEQUENCE;
	}

	private final IdGeneratorType type;
	private final String name;
	private final String keyColumnName;
	private final String valueColumnName;
	private final int hashCode;

	private IdGeneratorKeyMetadata(IdGeneratorType type, String name, String keyColumnName, String valueColumnName) {
		this.type = type;
		this.name = name;
		this.keyColumnName = keyColumnName;
		this.valueColumnName = valueColumnName;
		this.hashCode = calculateHashCode();
	}

	public static IdGeneratorKeyMetadata forTable(String table, String keyColumnName, String valueColumnName) {
		return new IdGeneratorKeyMetadata( IdGeneratorType.TABLE, table, keyColumnName, valueColumnName );
	}

	public static IdGeneratorKeyMetadata forSequence(String sequence) {
		return new IdGeneratorKeyMetadata( IdGeneratorType.SEQUENCE, sequence, null, null );
	}

	/**
	 * Returns the type of the represented id generator.
	 */
	public IdGeneratorType getType() {
		return type;
	}

	/**
	 * Returns the table name for table-based generators, the sequence name otherwise.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the key column name for table-based generators, {@code null} otherwise.
	 */
	public String getKeyColumnName() {
		return keyColumnName;
	}

	/**
	 * Returns the value column name for table-based generators, {@code null} otherwise.
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
		IdGeneratorKeyMetadata other = (IdGeneratorKeyMetadata) obj;
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
		return "IdGeneratorKeyMetadata [type=" + type + ", name=" + name + ", keyColumnName=" + keyColumnName + ", valueColumnName=" + valueColumnName + "]";
	}

	private int calculateHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
		result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
		return result;
	}
}
