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
package org.hibernate.ogm.dialect.couchdb.model;

import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.EntityKey;

import java.util.Set;

/**
 * Wraps a {@link Tuple} providing some utility methods
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class CouchDBTuple {

	private String[] columnNames;
	private Object[] columnValues;

	public CouchDBTuple() {
	}

	public CouchDBTuple(Tuple tuple) {
		setColumnNamesAndValues( tuple );
	}

	public CouchDBTuple(String[] columnNames, Object[] columnValues) {
		this.columnNames = columnNames;
		this.columnValues = columnValues;
	}

	public CouchDBTuple(Set<String> columnNames, Object[] columnValues) {
		setColumnNames( columnNames );
		this.columnValues = columnValues;
	}

	public CouchDBTuple(EntityKey entityKey) {
		columnNames = entityKey.getColumnNames();
		columnValues = entityKey.getColumnValues();
	}

	public String[] getColumnNames() {
		return columnNames;
	}

	public void setColumnNames(String[] columnNames) {
		this.columnNames = columnNames;
	}

	public void setColumnNames(Set<String> columnNames) {
		if ( columnNames != null ) {
			this.columnNames = columnNames.toArray( new String[columnNames.size()] );
		}
	}

	public Object[] getColumnValues() {
		return columnValues;
	}

	public void setColumnValues(Object[] columnValues) {
		this.columnValues = columnValues;
	}

	/**
	 * Return the position of the given column name
	 *
	 * @param columName
	 *            of the column which position is searched
	 * @return the position of the given columName
	 */
	public int getColumNamePosition(String columName) {
		for ( int i = 0; i < getColumnNames().length; i++ ) {
			if ( getColumnNames()[i].equals( columName ) ) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Return the value of the column at the given position
	 *
	 * @param position
	 *            of the column which value is searched
	 * @return the value of the column at the given position
	 */
	public Object getColumnValue(int position) {
		if ( position != -1 ) {
			return getColumnValues()[position];
		}
		return null;
	}

	/**
	 * Checks if the tuple is empty
	 *
	 * @return true if the Tuple contains no columns, false otherwise
	 */
	public boolean isEmpty() {
		return getColumnNames().length == 0;
	}

	private void setColumnNamesAndValues(Tuple tuple) {
		Set<String> tupleColumnNames = tuple.getColumnNames();
		String[] columnNames = new String[tupleColumnNames.size()];
		Object[] columnValues = new Object[tupleColumnNames.size()];
		int i = 0;
		for ( String columnName : tupleColumnNames ) {
			columnNames[i] = columnName;
			columnValues[i] = tuple.get( columnName );
			i++;
		}
		setColumnNames( columnNames );
		setColumnValues( columnValues );
	}

}
