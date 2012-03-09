/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.cassandra.impl;

import org.hibernate.HibernateException;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.datastore.cassandra.impl.CassandraTypeMapper;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ResultSetTupleSnapshot implements TupleSnapshot {
	private final ResultSet resultSet;
	private Map<String,Integer> columnNames = new HashMap<String,Integer>();
	private Table table;

	public ResultSetTupleSnapshot(ResultSet resultSet, Table tableMetadata) {
		this.table = tableMetadata;
		this.resultSet = resultSet;
		try {
			ResultSetMetaData metaData = resultSet.getMetaData();
			int count = metaData.getColumnCount();
			for(int index = 1 ; index <= count ; index++) {
				columnNames.put(metaData.getColumnName(index), index);
			}
		}
		catch (SQLException e) {
			throw new HibernateException("Unable to read resultset metadata", e);
		}
	}
	@Override
	public Object get(String column) {
		//because, by default, cassandra manages column name in lowercase
		Integer index = columnNames.get(column.toLowerCase());
		String type = CassandraTypeMapper.INSTANCE.getType( table, column );

		try {
			if ("blob".equalsIgnoreCase( type )) {
				return index == null ? null : resultSet.getBytes(index) ;
			}
			else if ( "int".equalsIgnoreCase( type ) ) {
				return index == null ? null : resultSet.getInt( index ) ;
			}
			else if ( "byte".equalsIgnoreCase( type ) ) {
				return index == null ? null : resultSet.getByte( index ) ;
			}
			else if ( "boolean".equalsIgnoreCase( type ) ) {
				return index == null ? null : resultSet.getBoolean( index ) ;
			}
			else if ( "float".equalsIgnoreCase( type ) ) {
				return index == null ? null : resultSet.getFloat( index ) ;
			}
			else if ( "bigint".equalsIgnoreCase( type ) ) {
				return index == null ? null : resultSet.getString( index ) ;
			}
			else if ( "calendar_date".equalsIgnoreCase( type ) ) {
				return index == null ? null : resultSet.getObject( index ) ;
			}
			else if ( "date".equalsIgnoreCase( type ) || ( "timestamp".equalsIgnoreCase( type )) || ( "time".equalsIgnoreCase( type )) ) {
				return index == null ? null : resultSet.getString( index ) ;
			}
			else if ( "decimal".equalsIgnoreCase( type ) ) {
				return index == null ? null : BigDecimal.valueOf( resultSet.getDouble( index ) ) ;
			}
			else if ( "long".equalsIgnoreCase( type ) ) {
				return index == null ? null : resultSet.getLong( index ) ;
			}
			else {
				return index == null ? null : resultSet.getString( index ) ;
			}

		} catch (SQLException e) {
			throw new HibernateException("Unable to read resultset column " + column, e);
		}
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
