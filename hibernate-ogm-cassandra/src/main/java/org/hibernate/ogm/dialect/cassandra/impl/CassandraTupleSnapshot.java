/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 *  JBoss, Home of Professional Open Source
 *  Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 *  as indicated by the @authors tag. All rights reserved.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This copyrighted material is made available to anyone wishing to use,
 *  modify, copy, or redistribute it subject to the terms and conditions
 *  of the GNU Lesser General Public License, v. 2.1.
 *  This program is distributed in the hope that it will be useful, but WITHOUT A
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *  You should have received a copy of the GNU Lesser General Public License,
 *  v.2.1 along with this distribution; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA  02110-1301, USA.
 */

package org.hibernate.ogm.dialect.cassandra.impl;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.driver.core.ColumnDefinitions;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Row;

import org.hibernate.HibernateException;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.type.CassandraTypeConverter;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class CassandraTupleSnapshot implements TupleSnapshot {
	private final List<Row> cqlRows;
	private final EntityKey entityKey;

	public CassandraTupleSnapshot(List<Row> cqlRows, EntityKey entityKey) {
		this.entityKey = entityKey;
		this.cqlRows = cqlRows;
	}

	@Override
	public Object get(String column) {
		if ( !isEmpty() ) {
			//TODO only for single pk
			String columName = column;
			//TODO : hack for @embedded usecase
			if (column.contains( "." )) {
				columName = column.replaceAll( "\\.", "_" );
			}

			DataType type = cqlRows.get( 0 ).getColumnDefinitions().getType( columName );

			return CassandraTypeConverter.getValue(cqlRows.get( 0 ), columName, type);
		}
		else {
			return null;
		}
	}

	@Override
	public boolean isEmpty() {
		return cqlRows == null | cqlRows.isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		Set<String> result = new HashSet<String>();

		// TODO only take the first?
		if ( !cqlRows.isEmpty() ) {
			ColumnDefinitions columnDefinitions = cqlRows.get( 0 ).getColumnDefinitions();
			for ( ColumnDefinitions.Definition columnDefinition : columnDefinitions ) {
				result.add( columnDefinition.getName() );
			}

		}
		return result;
	}
}
