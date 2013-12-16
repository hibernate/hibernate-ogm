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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.grid.impl.RowKeyBuilder;

/**
 * {@link AssociationSnapshot} implementation based on a
 * {@link org.hibernate.ogm.dialect.couchdb.backend.json.AssociationDocument} object as written to and retrieved from
 * the CouchDB server.
 *
 * @author Andrea Boriero <dreborier@gmail.com>
 * @author Gunnar Morling
 */
public class CouchDBAssociationSnapshot implements AssociationSnapshot {

	/**
	 * The original association representing this snapshot as retrieved from CouchDB.
	 */
	private final CouchDBAssociation couchDbAssociation;
	private final AssociationKey key;
	private final Map<RowKey, Map<String, Object>> rows = new HashMap<RowKey, Map<String, Object>>();

	public CouchDBAssociationSnapshot(CouchDBAssociation association, AssociationKey key) {
		this.couchDbAssociation = association;
		this.key = key;

		for ( Map<String, Object> row : association.getRows() ) {
			RowKey rowKey = new RowKeyBuilder()
					.tableName( key.getTable() )
					.addColumns( key.getRowKeyColumnNames() )
					.values( getRowKeyColumnValues( row, key ) )
					.build();

			rows.put( rowKey, row );
		}
	}

	/**
	 * Returns the values of the row key of the given association; columns present in the given association key will be
	 * obtained from there, all other columns from the given map.
	 */
	private static Map<String, Object> getRowKeyColumnValues(Map<String, Object> row, AssociationKey key) {
		Map<String, Object> rowKeyColumnValues = new HashMap<String, Object>();

		for ( String rowKeyColumnName : key.getRowKeyColumnNames() ) {
			rowKeyColumnValues.put(
					rowKeyColumnName,
					key.isKeyColumn( rowKeyColumnName ) ? key.getColumnValue( rowKeyColumnName ) : row.get( rowKeyColumnName )
			);
		}

		return rowKeyColumnValues;
	}

	@Override
	public boolean containsKey(RowKey column) {
		return rows.containsKey( column );
	}

	@Override
	public Tuple get(RowKey column) {
		Map<String, Object> row = rows.get( column );
		return row != null ? new Tuple( new CouchDBAssociationRowTupleSnapshot( row, key ) ) : null;
	}

	@Override
	public int size() {
		return rows.size();
	}

	@Override
	public Set<RowKey> getRowKeys() {
		return rows.keySet();
	}

	public CouchDBAssociation getCouchDbAssociation() {
		return couchDbAssociation;
	}

	@Override
	public String toString() {
		return "CouchDBAssociationSnapshot [key=" + key + ", rows=" + rows + "]";
	}

	private static class CouchDBAssociationRowTupleSnapshot implements TupleSnapshot {

		private final Map<String, Object> associationRow;
		private final AssociationKey associationKey;
		private final Set<String> columnNames;

		private CouchDBAssociationRowTupleSnapshot(Map<String, Object> associationRow, AssociationKey associationKey) {
			this.associationRow = associationRow;
			this.associationKey = associationKey;
			this.columnNames = getColumnNames( associationRow, associationKey );
		}

		private static Set<String> getColumnNames(Map<String, Object> associationRow, AssociationKey associationKey) {
			Set<String> columnNames = new HashSet<String>( associationRow.size() + associationKey.getColumnNames().length );
			columnNames.addAll( associationRow.keySet() );
			for ( String column : associationKey.getColumnNames() ) {
				columnNames.add( column );
			}

			return columnNames;
		}

		@Override
		public Object get(String column) {
			return associationKey.isKeyColumn( column ) ? associationKey.getColumnValue( column ) : associationRow.get( column );
		}

		@Override
		public boolean isEmpty() {
			return columnNames.isEmpty();
		}

		@Override
		public Set<String> getColumnNames() {
			return columnNames;
		}
	}
}
