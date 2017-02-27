/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.persistencestrategy.table.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.hibernate.ogm.datastore.ehcache.persistencestrategy.common.impl.VersionChecker;
import org.hibernate.ogm.model.key.spi.IdSourceKey;

/**
 * Used to serialize {@link IdSourceKey} objects in Ehcache.
 *
 * @author Gunnar Morling
 */
public final class PerTableSerializableIdSourceKey implements Externalizable {

	/**
	 * NEVER change this, as otherwise serialized representations cannot be read back after an update
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * To be incremented when the structure of this type changes. Based on the version of a serialized representation,
	 * specific handling can be implemented in {@link #readExternal(ObjectInput)}.
	 */
	private static final int VERSION = 1;

	private String columnName;
	private Object columnValue;

	// required by Externalizable
	public PerTableSerializableIdSourceKey() {
	}

	PerTableSerializableIdSourceKey(IdSourceKey key) {
		columnName = key.getColumnName();
		columnValue = key.getColumnValue();
	}

	public String getColumnNames() {
		return columnName;
	}

	public Object getColumnValues() {
		return columnValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( columnName == null ) ? 0 : columnName.hashCode() );
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
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		PerTableSerializableIdSourceKey other = (PerTableSerializableIdSourceKey) obj;
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
		return true;
	}

	@Override
	public String toString() {
		return "PerTableSerializableIdSourceKey [columnName='" + columnName + "', columnValues='" + columnValue + "']";
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt( VERSION );
		//Wrapping in String[] and Object[] respectively as this used to be the format,
		//to maintain compatibility with Hibernate OGM 5.0
		out.writeObject( new String[] { columnName } );
		out.writeObject( new Object[] { columnValue } );
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		VersionChecker.readAndCheckVersion( in, VERSION, PerTableSerializableIdSourceKey.class );

		columnName = ( (String[]) in.readObject() )[0];
		columnValue = ( (Object[]) in.readObject() )[0];
	}

}
