/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.model.impl;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.Row;

import org.hibernate.ogm.model.spi.TupleSnapshot;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Wrap a java-driver Row, handling column name and type mappings.
 *
 * @author Jonathan Halliday
 */
public class ResultSetTupleSnapshot implements TupleSnapshot {

	private final Row row;
	private Map<String, Integer> columnNames = new HashMap<String, Integer>();
	private ProtocolVersion protocolVersion;

	public ResultSetTupleSnapshot(Row row, ProtocolVersion protocolVersion) {
		this.row = row;
		this.protocolVersion = protocolVersion;

		ColumnDefinitions columnDefinitions = row.getColumnDefinitions();
		int count = columnDefinitions.size();
		for ( int index = 0; index < count; index++ ) {
			columnNames.put( columnDefinitions.getName( index ), index );
		}
	}

	@Override
	public Object get(String column) {

		Integer index = columnNames.get( column );
		DataType dataType = row.getColumnDefinitions().getType( index );
		ByteBuffer byteBuffer = row.getBytesUnsafe( index );
		if ( byteBuffer == null ) {
			return null;
		}
		Object value = dataType.deserialize( byteBuffer, protocolVersion );
		return value;
	}

	@Override
	public boolean isEmpty() {
		return columnNames.isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		return columnNames.keySet();
	}
}
