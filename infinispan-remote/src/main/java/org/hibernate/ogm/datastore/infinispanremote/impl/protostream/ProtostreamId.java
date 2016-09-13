/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protostream;

import java.util.Arrays;
import java.util.Objects;

import org.hibernate.ogm.model.key.spi.RowKey;

public final class ProtostreamId {

	public final NamedValue[] namedValues;
	//Redundant information for convenience, as we might need to represent in either form:
	public final String[] columnNames;
	public final Object[] columnValues;

	public ProtostreamId(String[] columnNames, Object[] columnValues) {
		this.columnNames = columnNames;
		this.columnValues = columnValues;
		Objects.requireNonNull( columnNames );
		Objects.requireNonNull( columnValues );
		if ( columnNames.length != columnValues.length ) {
			throw new IllegalArgumentException( "The size of the arrays of the two parameters is required to be the same" );
		}
		namedValues = new NamedValue[ columnNames.length ];
		for ( int i = 0; i < columnNames.length; i++ ) {
			namedValues[i] = new NamedValue( columnNames[i], columnValues[i] );
		}
	}

	public RowKey toRowKey() {
		return new RowKey( columnNames, columnValues );
	}

	public static final class NamedValue {
		public final String columnName;
		public final Object columnValue;
		public NamedValue(String columnName, Object columnValue) {
			this.columnName = Objects.requireNonNull( columnName );
			this.columnValue = columnValue;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + columnName.hashCode();
			result = prime * result + ( ( columnValue == null ) ? 0 : columnValue.hashCode() );
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( NamedValue.class != obj.getClass() ) {
				return false;
			}
			NamedValue other = (NamedValue) obj;
			if ( ! columnName.equals( other.columnName ) ) {
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
			return true;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode( namedValues );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( ProtostreamId.class != obj.getClass() ) {
			return false;
		}
		ProtostreamId other = (ProtostreamId) obj;
		if ( ! Arrays.equals( namedValues, other.namedValues ) ) {
			return false;
		}
		return true;
	}

}
