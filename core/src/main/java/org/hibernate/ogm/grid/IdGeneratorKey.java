/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.grid;

import java.util.Arrays;

/**
 * Represents a table- or sequence-based id generator.
 *
 * @author Gunnar Morling
 */
public class IdGeneratorKey implements Key {

	private final IdGeneratorKeyMetadata metadata;
	private final String[] columnNames;
	private final Object[] columnValues;
	private final int hashCode;

	private IdGeneratorKey(IdGeneratorKeyMetadata metadata, Object[] columnValues) {
		this.metadata = metadata;
		this.columnNames = metadata.getKeyColumnName() != null ? new String[] { metadata.getKeyColumnName() } : null;
		this.columnValues = columnValues;
		this.hashCode = calculateHashCode();
	}

	public static IdGeneratorKey forTable(IdGeneratorKeyMetadata metadata, Object segmentName) {
		return new IdGeneratorKey( metadata, new Object[] { segmentName } );
	}

	public IdGeneratorKeyMetadata getMetadata() {
		return metadata;
	}

	/**
	 * Returns the table name if this is a table-based generator, the sequence name otherwise.
	 */
	@Override
	public String getTable() {
		return metadata.getName();
	}

	/**
	 * Returns the segment column name if this is a table-based generator, {@code null} otherwise.
	 */
	@Override
	public String[] getColumnNames() {
		return columnNames;
	}

	/**
	 * Returns the segment name if this is a table-based generator, {@code null} otherwise.
	 */
	@Override
	public Object[] getColumnValues() {
		return columnValues;
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
		IdGeneratorKey other = (IdGeneratorKey) obj;
		if ( !Arrays.equals( columnValues, other.columnValues ) ) {
			return false;
		}
		if ( metadata == null ) {
			if ( other.metadata != null ) {
				return false;
			}
		}
		else if ( !metadata.equals( other.metadata ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "IdGeneratorKey [metadata=" + metadata + ", columnNames=" + Arrays.toString( columnNames ) + ", columnValues=" + Arrays.toString( columnValues )
				+ "]";
	}

	private int calculateHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode( columnValues );
		result = prime * result + ( ( metadata == null ) ? 0 : metadata.hashCode() );
		return result;
	}
}
