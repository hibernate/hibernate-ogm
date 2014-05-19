/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.grid;

import java.util.Arrays;

/**
 *A key representing an association row or identifier sequence.
 *
 * @author Emmanuel Bernard
 */
public final class RowKey implements Key {

	private final String table;
	private final String[] columnNames;
	//column value types do have to be serializable so RowKey can be serializable
	//should it be a Serializable[] type? It seems to be more pain than anything else
	private final Object[] columnValues;
	private final int hashCode;

	public RowKey(String table, String[] columnNames, Object[] columnValues) {
		this.table = table;
		this.columnNames = columnNames;
		this.columnValues = columnValues;
		this.hashCode = generateHashCode();
	}

	@Override
	public String getTable() {
		return table;
	}

	/**
	 * This class should be treated as immutable. While we expose this array,
	 * you should never make changes to it!
	 * This is a design tradeoff vs. raw performance and memory usage.
	 */
	@Override
	public String[] getColumnNames() {
		return columnNames;
	}

	/**
	 * This class should be treated as immutable. While we expose this array,
	 * you should never make changes to it!
	 * This is a design tradeoff vs. raw performance and memory usage.
	 */
	@Override
	public Object[] getColumnValues() {
		return columnValues;
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

		if ( !table.equals( that.table ) ) {
			return false;
		}

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
		final int result = table.hashCode();
		return 31 * result + Arrays.hashCode( columnValues );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "RowKey" );
		sb.append( "{table='" ).append( table ).append( '\'' );
		sb.append( ", columnNames=" ).append( columnNames == null ? "null" : Arrays.asList( columnNames ).toString() );
		sb.append( ", columnValues=" )
				.append( columnValues == null ? "null" : Arrays.asList( columnValues ).toString() );
		sb.append( '}' );
		return sb.toString();
	}
}
