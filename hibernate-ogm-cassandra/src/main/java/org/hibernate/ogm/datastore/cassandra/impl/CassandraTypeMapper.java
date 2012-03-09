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
package org.hibernate.ogm.datastore.cassandra.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;

/**
 * Helper class used to map Cassandra inner type and java type
 * TODO : quite ugly but there should be a better solution?
 * @author Khanh Tuong Maudoux
 */
public enum CassandraTypeMapper {
	INSTANCE;

	public static final String UNKNOWN_TYPE = "unknown";

	public Map<String, String> mapper = new HashMap<String, String>(  );
	{
		mapper.put( "materialized_blob", "blob" );
		mapper.put( "int", "int" );
		mapper.put( "date", "date" );
		mapper.put( "calendar", "calendar_date" );
		mapper.put( "calendar_date", "calendar_date" );
		mapper.put( "java.lang.Byte", "byte" );
		mapper.put( "java.lang.Boolean", "boolean" );
		mapper.put( "java.util.UUID", "uuid" );
		mapper.put( "java.math.BigDecimal", "decimal" );
		mapper.put( "java.lang.Integer", "int" );
		mapper.put( "java.math.BigInteger", "bigint" );
		mapper.put( "java.lang.Long", "long" );
		mapper.put( "java.lang.Float", "float" );
		mapper.put( "java.net.URL", "varchar" );
	}


	public String getType(CassandraDatastoreProvider provider, String tableName, String columnName) {
		Table table = provider.getMetaDataCache().get( tableName );
		Iterator<Column> columnsIt = (Iterator<Column>) table.getColumnIterator();

		while (columnsIt.hasNext()) {
			Column column = columnsIt.next();
			if (columnName.equalsIgnoreCase( column.getName() ) ) {
				String columnType = ((SimpleValue) column.getValue()).getTypeName();
				String innerType = CassandraTypeMapper.INSTANCE.mapper.get( columnType );
				return (innerType == null) ? "varchar" : innerType;

			}
		}
		return CassandraTypeMapper.UNKNOWN_TYPE;
	}

	public String getType(Table table, String columnName) {
		Iterator<Column> columnsIt = (Iterator<Column>) table.getColumnIterator();

		while (columnsIt.hasNext()) {
			Column column = columnsIt.next();
			if (columnName.equalsIgnoreCase( column.getName() ) ) {
				String columnType = ((SimpleValue) column.getValue()).getTypeName();
				String innerType = CassandraTypeMapper.INSTANCE.mapper.get( columnType );

				return (innerType == null) ? "varchar" : innerType;
			}
		}
		return CassandraTypeMapper.UNKNOWN_TYPE;
	}
}
