/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.grid.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.RowKey;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class RowKeyBuilder {
	private List<String> columnNames = new ArrayList<String>();
	private Map<String,Object> values;
	private String tableName;
	private Tuple tuple;

	public RowKeyBuilder addColumns(String... columns) {
		for(String columnName : columns ) {
			columnNames.add( columnName );
		}
		return this;
	}

	public RowKeyBuilder values(Map<String,Object> values) {
		this.values = values;
		return this;
	}

	public RowKeyBuilder tableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public RowKey build() {
		final String[] columnNamesArray = columnNames.toArray( new String[columnNames.size()] );
		final int length = columnNamesArray.length;
		Object[] columnValuesArray = new Object[length];
		if (values != null) {
			for (int index = 0 ; index < length ; index++ ) {
				columnValuesArray[index] = values.get( columnNamesArray[index] );
			}
		}
		else {
			for (int index = 0 ; index < length ; index++ ) {
				columnValuesArray[index] = tuple.get( columnNamesArray[index] );
			}
		}
		return new RowKey( tableName, columnNamesArray, columnValuesArray );
	}

	public RowKeyBuilder values(Tuple tuple) {
		this.tuple = tuple;
		return this;
	}

	public String[] getColumnNames() {
		return columnNames.toArray( new String[ columnNames.size() ] );
	}
}
