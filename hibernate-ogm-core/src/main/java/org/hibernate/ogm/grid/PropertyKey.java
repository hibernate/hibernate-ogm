package org.hibernate.ogm.grid;

import java.io.Serializable;

/**
 * Represents the key used to link a property value and the id of it's owning entity
 *
 * @author Emmanuel Bernard
 */
public class PropertyKey {
	private final String table;
	//TODO replace property with columns
	private final String property;
	//TODO replace serializable with Map<String,Object> column values
	private final Serializable value;

	public PropertyKey(String table, String property, Serializable value) {
		this.table = table;
		this.property = property;
		this.value = value;
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

		if ( property != null ? !property.equals( that.property ) : that.property != null ) {
			return false;
		}
		if ( table != null ? !table.equals( that.table ) : that.table != null ) {
			return false;
		}
		if ( value != null ? !value.equals( that.value ) : that.value != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = table != null ? table.hashCode() : 0;
		result = 31 * result + ( property != null ? property.hashCode() : 0 );
		result = 31 * result + ( value != null ? value.hashCode() : 0 );
		return result;
	}
}
