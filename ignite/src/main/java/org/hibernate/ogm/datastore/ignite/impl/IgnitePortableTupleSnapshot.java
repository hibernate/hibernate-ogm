package org.hibernate.ogm.datastore.ignite.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ignite.binary.BinaryObject;
import org.hibernate.ogm.model.spi.TupleSnapshot;

public class IgnitePortableTupleSnapshot implements TupleSnapshot {

	private final BinaryObject binaryObject;
	private final Set<String> columnNames;

	public IgnitePortableTupleSnapshot(Object binaryObject) {
		this.binaryObject = (BinaryObject) binaryObject;
		if (binaryObject != null) {
			this.columnNames = new HashSet<String>( this.binaryObject.type().fieldNames() );
		}
		else {
			this.columnNames = Collections.emptySet();
		}
	}

	@Override
	public Object get(String column) {
		return !isEmpty() ? binaryObject.field( column ) : null;
	}

	@Override
	public boolean isEmpty() {
		return binaryObject == null;
	}

	@Override
	public Set<String> getColumnNames() {
		return columnNames;
	}
}