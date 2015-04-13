/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.model.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.RowKey;

/**
 * Wrap a java-driver Row, handling column name and type mappings.
 *
 * @author Jonathan Halliday
 */
public class ResultSetAssociationSnapshot implements AssociationSnapshot {

	private final Table table;
	private Map<RowKey, Row> res = new HashMap<RowKey, Row>();
	private com.datastax.driver.core.ProtocolVersion protocolVersion;

	public ResultSetAssociationSnapshot(
			AssociationKey key,
			ResultSet resultSet,
			Table tableMetadata,
			ProtocolVersion protocolVersion) {
		this.table = tableMetadata;

		if ( resultSet == null ) {
			res = Collections.EMPTY_MAP;
			return;
		}

		List<String> combinedKeys = new LinkedList<String>();
		combinedKeys.addAll( Arrays.asList( key.getColumnNames() ) );
		for ( Object column : tableMetadata.getPrimaryKey().getColumns() ) {
			String name = ((Column) column).getName();
			if ( !combinedKeys.contains( name ) ) {
				combinedKeys.add( name );
			}
		}
		String[] columnNames = combinedKeys.toArray( new String[combinedKeys.size()] );

		for ( Row row : resultSet ) {
			Object[] columnValues = new Object[columnNames.length];
			for ( int i = 0; i < columnNames.length; i++ ) {
				DataType dataType = row.getColumnDefinitions().getType( columnNames[i] );
				columnValues[i] = dataType.deserialize( row.getBytesUnsafe( columnNames[i] ), protocolVersion );
			}
			RowKey rowKey = new RowKey( columnNames, columnValues );
			res.put( rowKey, row );
		}
	}

	@Override
	public Tuple get(RowKey rowKey) {
		return new Tuple( new ResultSetTupleSnapshot( res.get( rowKey ), protocolVersion ) );
	}

	@Override
	public boolean containsKey(RowKey rowKey) {
		return res.containsKey( rowKey );
	}

	@Override
	public int size() {
		return res.size();
	}

	@Override
	public Set<RowKey> getRowKeys() {
		return res.keySet();
	}
}
