/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.key.spi;

import java.util.Arrays;

/**
 *A key representing an association row or identifier sequence.
 *
 * @author Emmanuel Bernard
 */
public final class RowKey {

	private final String[] columnNames;
	//column value types do have to be serializable so RowKey can be serializable
	//should it be a Serializable[] type? It seems to be more pain than anything else
	private final Object[] columnValues;
	private final int hashCode;

	public RowKey(String[] columnNames, Object[] columnValues) {
		this.columnNames = columnNames;
		this.columnValues = columnValues;
		this.hashCode = generateHashCode();
	}

	/**
	 * This class should be treated as immutable. While we expose this array,
	 * you should never make changes to it!
	 * This is a design tradeoff vs. raw performance and memory usage.
	 */
	public String[] getColumnNames() {
		return columnNames;
	}

	/**
	 * This class should be treated as immutable. While we expose this array,
	 * you should never make changes to it!
	 * This is a design tradeoff vs. raw performance and memory usage.
	 */
	public Object[] getColumnValues() {
		return columnValues;
	}

	/**
	 * @return the corresponding value of the column, null if the column does not exist in the row key
	 */
	public Object getColumnValue(String columnName) {
		for ( int j = 0; j < columnNames.length; j++ ) {
			if ( columnNames[j].equals( columnName ) ) {
				return columnValues[j];
			}
		}
		return null;
	}

	/**
	 * @return true if the column is one of the row key columns, false otherwise
	 */
	public boolean contains(String column) {
		for ( String columnName : columnNames ) {
			if ( columnName.equals( column ) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || RowKey.class != o.getClass() ) {
			return false;
		}

		RowKey that = (RowKey) o;

		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if ( !Arrays.equals( columnValues, that.columnValues ) ) {
			return false;
		}
		if ( !Arrays.equals( columnNames, that.columnNames ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	private int generateHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode( columnNames );
		result = prime * result + Arrays.hashCode( columnValues );
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "RowKey[" );
		int i = 0;
		for ( String column : columnNames ) {
			sb.append( column ).append( "=" ).append( columnValues[i] );
			i++;
			if ( i < columnNames.length ) {
				sb.append( ", " );
			}
		}
		sb.append( "]" );
		return sb.toString();
	}
}
