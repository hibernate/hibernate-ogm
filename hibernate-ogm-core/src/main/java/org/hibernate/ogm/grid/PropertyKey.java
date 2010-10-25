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
	//TODO replace property with columns
	private final String property;
	private final Object[] columnValues;

	public PropertyKey(String table, String property, Object[] columnValues) {
		this.table = table;
		this.property = property;
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

		if ( !Arrays.equals( columnValues, that.columnValues ) ) {
			return false;
		}
		if ( property != null ? !property.equals( that.property ) : that.property != null ) {
			return false;
		}
		if ( table != null ? !table.equals( that.table ) : that.table != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = table != null ? table.hashCode() : 0;
		result = 31 * result + ( property != null ? property.hashCode() : 0 );
		result = 31 * result + ( columnValues != null ? Arrays.hashCode( columnValues ) : 0 );
		return result;
	}
}
