package org.hibernate.ogm.datastore.ignite.persistencestrategy;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import org.hibernate.ogm.model.key.spi.EntityKey;

/**
 * Class for serialize EntityKey in Ignite cache
 * @author Dmitriy Kozlov
 *
 */
public class IgniteSerializableEntityKey implements Externalizable {

	private String[] columnNames;
	private Object[] columnValues;
	
	/**
	 * To be incremented when the structure of this type changes. Based on the version of a serialized representation,
	 * specific handling can be implemented in {@link #readExternal(ObjectInput)}.
	 */
	private static final int VERSION = 1;
	
	public IgniteSerializableEntityKey() {
	}
	
	public IgniteSerializableEntityKey(EntityKey key) {
		columnNames = key.getColumnNames();
		columnValues = key.getColumnValues();
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
		result = prime * result + Arrays.hashCode(columnNames);
		result = prime * result + Arrays.hashCode(columnValues);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IgniteSerializableEntityKey other = (IgniteSerializableEntityKey) obj;
		if (!Arrays.equals(columnNames, other.columnNames))
			return false;
		if (!Arrays.equals(columnValues, other.columnValues))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "IgniteSerializableEntityKey [columnNames="
				+ Arrays.toString(columnNames) + ", columnValues="
				+ Arrays.toString(columnValues) + "]";
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		VersionChecker.readAndCheckVersion( in, VERSION, IgniteSerializableEntityKey.class );

		columnNames = (String[]) in.readObject();
		columnValues = (Object[]) in.readObject();
	}
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt( VERSION );
		out.writeObject( columnNames );
		out.writeObject( columnValues );
	}

	
	
}
