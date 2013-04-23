/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.dialect.couchdb.resteasy;

import org.hibernate.ogm.grid.RowKey;

import java.util.Map;

/**
 * Represents the column values of a {@link org.hibernate.ogm.datastore.spi.Tuple} of an
 * {@link org.hibernate.ogm.datastore.spi.Association}
 *
 * @author Andrea Boriero <dreborier@gmail.com>
 */
class AssociationTuple {
	private Object[] tupleColumnValues;

	AssociationTuple() {
	}

	public AssociationTuple(Object[] tupleColumnValues) {
		this.tupleColumnValues = tupleColumnValues;
	}

	public Object[] getTupleColumnValues() {
		return tupleColumnValues;
	}

	/**
	 * Check if the AssociationTuple contains the {@link RowKey}.
	 *
	 * @param columnNamesPositions
	 *            give the position of each column name
	 * @param key
	 * @return true if the AssociationTuple contains the key
	 */
	public boolean hasKey(Map<String, Integer> columnNamesPositions, RowKey key) {
		return hasTupleAllTheKeyColumnValues( columnNamesPositions, key.getColumnNames(), key.getColumnValues() );
	}

	/**
	 * Construct the RowKey from the AssociationTuple
	 *
	 * @param rowKeycolumnNames
	 * @param tableName
	 * @param columnNamesPositions
	 * @return the RowKey
	 */
	public RowKey getRowKey(String[] rowKeycolumnNames, String tableName, Map<String, Integer> columnNamesPositions) {
		Object[] rowKeyColumnValues = new Object[rowKeycolumnNames.length];
		for ( int i = 0; i < rowKeycolumnNames.length; i++ ) {
			rowKeyColumnValues[i] = getValueForColumn( columnNamesPositions.get( rowKeycolumnNames[i] ) );
		}
		return new RowKey( tableName, rowKeycolumnNames, rowKeyColumnValues );
	}

	public void setTupleColumnValues(Object[] tupleColumnValues) {
		this.tupleColumnValues = tupleColumnValues;
	}

	private boolean hasTupleAllTheKeyColumnValues(Map<String, Integer> columnNamesPositions, String[] keyColumnNames,
			Object[] keyColumnValues) {
		for ( int i = 0; i < keyColumnNames.length; i++ ) {
			final int position = columnNamesPositions.get( keyColumnNames[i] );
			if ( !hasTupleTheColumnValue( position, keyColumnValues[i] ) ) {
				return false;
			}
		}
		return true;
	}

	private boolean hasTupleTheColumnValue(int position, Object columnValue) {
		return tupleColumnValues[position].equals( columnValue );
	}

	private Object getValueForColumn(int columPosition) {
		return tupleColumnValues[columPosition];
	}

}
