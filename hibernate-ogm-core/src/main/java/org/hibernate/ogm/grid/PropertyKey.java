package org.hibernate.ogm.grid;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents the key used to link a property value and the id of it's owning entity
 *
 * @author Emmanuel Bernard
 */
public class PropertyKey {
	private final String table;
	private final String[] columns;
	private final Object[] columnValues;

	public PropertyKey(String table, String[] columns, Object[] columnValues) {
		this.table = table;
		this.columns = columns;
		this.columnValues = columnValues;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		PropertyKey that = ( PropertyKey ) o;

		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if ( !Arrays.equals( columnValues, that.columnValues ) ) {
			return false;
		}
		if ( !Arrays.equals( columns, that.columns ) ) {
			return false;
		}
		if ( !table.equals( that.table ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = table.hashCode();
		result = 31 * result + Arrays.hashCode( columns );
		result = 31 * result + Arrays.hashCode( columnValues );
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "PropertyKey" );
		sb.append( "{table='" ).append( table ).append( '\'' );
		sb.append( ", columns=" ).append( columns == null ? "null" : Arrays.asList( columns ).toString() );
		sb.append( ", columnValues=" )
				.append( columnValues == null ? "null" : Arrays.asList( columnValues ).toString() );
		sb.append( '}' );
		return sb.toString();
	}
}
