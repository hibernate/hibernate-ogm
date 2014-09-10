/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.dialect.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import org.hibernate.ogm.model.key.spi.AssociationKey;

/**
 * Used to serialize {@link AssociationKey} objects in Ehcache.
 *
 * @author Gunnar Morling
 */
public class SerializableAssociationKey implements Externalizable {

	/**
	 * NEVER change this, as otherwise serialized representations cannot be read back after an update
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * To be incremented when the structure of this type changes. Based on the version of a serialized representation,
	 * specific handling can be implemented in {@link #readExternal(ObjectInput)}.
	 */
	private static final int VERSION = 1;

	private String table;
	private String[] columnNames;
	private Object[] columnValues;

	// required by Externalizable
	public SerializableAssociationKey() {
	}

	public SerializableAssociationKey(AssociationKey key) {
		columnNames = key.getColumnNames();
		columnValues = key.getColumnValues();
		table = key.getTable();
	}

	public String getTable() {
		return table;
	}

	public String[] getColumnNames() {
		return columnNames;
	}

	public Object[] getColumnValues() {
		return columnValues;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode( columnNames );
		result = prime * result + Arrays.hashCode( columnValues );
		result = prime * result + ( ( table == null ) ? 0 : table.hashCode() );
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
		SerializableAssociationKey other = (SerializableAssociationKey) obj;
		if ( !Arrays.equals( columnNames, other.columnNames ) ) {
			return false;
		}
		if ( !Arrays.equals( columnValues, other.columnValues ) ) {
			return false;
		}
		if ( table == null ) {
			if ( other.table != null ) {
				return false;
			}
		}
		else if ( !table.equals( other.table ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "SerializableAssociationKey [table=" + table + ", columnNames=" + Arrays.toString( columnNames ) + ", columnValues="
				+ Arrays.toString( columnValues ) + "]";
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt( VERSION );
		out.writeUTF( table );
		out.writeObject( columnNames );
		out.writeObject( columnValues );
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		// version
		in.readInt();

		table = in.readUTF();
		columnNames = (String[]) in.readObject();
		columnValues = (Object[]) in.readObject();
	}
}
