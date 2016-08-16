/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.key.spi;

/**
 * Represents a source of an id sequence such as a table (row) or a physical sequence.
 * This is used to identify a Sequence or a Table based id generator.
 * At most a single column is needed: columnName and columnValue to map to the attributes of
 * JPA's TableGenerated.
 *
 * @author Gunnar Morling
 * @author Sanne Grinovero
 */
public final class IdSourceKey {

	private final IdSourceKeyMetadata metadata;
	private final String columnName;
	private final String columnValue;
	private final int hashCode;

	private IdSourceKey(IdSourceKeyMetadata metadata, String columnValue) {
		this.metadata = metadata;
		this.columnName = metadata.getKeyColumnName();
		this.columnValue = columnValue;
		this.hashCode = calculateHashCode();
	}

	public static IdSourceKey forTable(IdSourceKeyMetadata metadata, String segmentName) {
		return new IdSourceKey( metadata, segmentName );
	}

	public static IdSourceKey forSequence(IdSourceKeyMetadata metadata) {
		return new IdSourceKey( metadata, null );
	}

	public IdSourceKeyMetadata getMetadata() {
		return metadata;
	}

	/**
	 * Get the table name if this is a table-based generator, the sequence name otherwise.
	 *
	 * @return the table or the sequence name
	 */
	public String getTable() {
		return metadata.getName();
	}

	/**
	 * Get the segment column name if this is a table-based generator, {@code null} otherwise.
	 *
	 * @return the segment column name when using a table-based generator, {@code null} otherwise
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * Get the segment name if this is a table-based generator, {@code null} otherwise.
	 *
	 * @return the segment name when using a table-based generator, {@code null} otherwise
	 */
	public String getColumnValue() {
		return columnValue;
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
		if ( IdSourceKey.class != obj.getClass() ) {
			return false;
		}
		IdSourceKey other = (IdSourceKey) obj;
		if ( columnName == null ) {
			if ( other.columnName != null ) {
				return false;
			}
		}
		else if ( ! columnName.equals( other.columnName ) ) {
			return false;
		}
		if ( columnValue == null ) {
			if ( other.columnValue != null ) {
				return false;
			}
		}
		else if ( ! columnValue.equals( other.columnValue ) ) {
			return false;
		}
		if ( hashCode != other.hashCode ) {
			return false;
		}
		if ( metadata == null ) {
			if ( other.metadata != null ) {
				return false;
			}
		}
		else if ( ! metadata.equals( other.metadata ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "IdSourceKey [metadata=" + metadata + ", columnName='" + columnName + "', columnValue='" + columnValue + "']";
	}

	private int calculateHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( (columnName == null) ? 0 : columnName.hashCode() );
		result = prime * result + ( (columnValue == null) ? 0 : columnValue.hashCode() );
		result = prime * result + ( (metadata == null) ? 0 : metadata.hashCode() );
		return result;
	}

}
