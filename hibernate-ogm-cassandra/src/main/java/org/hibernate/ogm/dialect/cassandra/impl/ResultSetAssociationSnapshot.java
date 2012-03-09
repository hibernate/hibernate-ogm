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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.RowKey;

/**
 * @author Khanh Tuong Maudoux
 */
public class ResultSetAssociationSnapshot implements AssociationSnapshot {
	private final ResultSet resultSet;
	private Map<RowKey, Map<String, Integer>> res = new HashMap<RowKey, Map<String, Integer>>();
	private Table table;

	public ResultSetAssociationSnapshot(RowKey rowKey, ResultSet resultSet, Table tableMetadata) {
		this.table = tableMetadata;
		this.resultSet = resultSet;
		try {
			ResultSetMetaData metaData = resultSet.getMetaData();
			int count = metaData.getColumnCount();
			Map<String, Integer> columnNames = new HashMap<String, Integer>();
			for ( int index = 1; index <= count; index++ ) {
				String columnName = metaData.getColumnName( index );
				columnNames.put( columnName, index );
			}
			res.put( rowKey, columnNames );
		}
		catch (SQLException e) {
			throw new HibernateException( "Unable to read resultset metadata", e );
		}
	}


	@Override
	public Tuple get(RowKey column) {
		return new Tuple( new ResultSetTupleSnapshot( resultSet, table ) );
	}

	@Override
	public boolean containsKey(RowKey column) {
		return this.res.containsKey( column );
	}

	@Override
	public int size() {
		return this.res.size();
	}

	@Override
	public Set<RowKey> getRowKeys() {
		return res.keySet();
	}
}
