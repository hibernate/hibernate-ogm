/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class RowKeyBuilder {
	private final List<String> columnNames = new ArrayList<String>();
	private final List<String> indexColumnNames = new ArrayList<String>( 3 );
	private Tuple tuple;

	public RowKeyBuilder addColumns(String... columns) {
		for ( String columnName : columns ) {
			columnNames.add( columnName );
		}
		return this;
	}

	public RowKeyBuilder addIndexColumns(String... columns) {
		for ( String columnName : columns ) {
			columnNames.add( columnName );
			indexColumnNames.add( columnName );
		}
		return this;
	}

	public RowKey build() {
		final String[] columnNamesArray = columnNames.toArray( new String[columnNames.size()] );
		final int length = columnNamesArray.length;
		Object[] columnValuesArray = new Object[length];

		for (int index = 0 ; index < length ; index++ ) {
			columnValuesArray[index] = tuple.get( columnNamesArray[index] );
		}

		return new RowKey( columnNamesArray, columnValuesArray );
	}

	public RowKeyBuilder values(Tuple tuple) {
		this.tuple = tuple;
		return this;
	}

	public String[] getColumnNames() {
		return columnNames.toArray( new String[ columnNames.size() ] );
	}

	public String[] getIndexColumnNames() {
		return indexColumnNames.toArray( new String[ indexColumnNames.size() ] );
	}
}
