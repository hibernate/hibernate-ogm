/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.model.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.grid.impl.RowKeyBuilder;
import org.hibernate.ogm.util.impl.Contracts;

/**
 * {@link AssociationSnapshot} implementation based on a
 * {@link org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.AssociationDocument} object as written to and retrieved from
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

		AssociationRows associationRows = new AssociationRows( association.getRows(), key );
		for ( Map<String, Object> row : associationRows ) {
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

	/**
	 * An {@link Iterable} representing the rows of an association in form of {@link DBObject}s.
	 * <p>
	 * If the rows have more than one column, they will be returned as {@code DBObject}s by the driver already and be
	 * passed through as is.
	 * <p>
	 * If the rows only have one column (which is the case for collections of simple values such as {@code int}s,
	 * {@code String}s etc. as well as associations based on non-composite keys), a {@code DBObject} will be created
	 * using the column value and the single row key column from the association's key which is not part of the
	 * association key.
	 *
	 * @author Gunnar Morling
	 */
	private static class AssociationRows implements Iterable<Map<String, Object>> {

		private final Iterator<?> wrapped;
		private final int size;
		private final String fieldName;

		public AssociationRows(Collection<?> wrapped, AssociationKey key) {
			this.wrapped = wrapped.iterator();
			this.size = wrapped.size();
			this.fieldName = getSingleRowKeyColumnNotContainedInAssociationKey( key );
		}

		@Override
		public Iterator<Map<String, Object>> iterator() {
			return new Iterator<Map<String, Object>>() {

				@Override
				public Map<String, Object> next() {
					Object next = wrapped.next();
					if ( next instanceof Map ) {
						return (Map<String, Object>) next;
					}
					else {
						Contracts.assertNotNull( fieldName, "fieldName" );
						return Collections.singletonMap( fieldName, next );
					}
				}

				@Override
				public boolean hasNext() {
					return wrapped.hasNext();
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException( "Removal is not supported" );
				}
			};
		}

		public int size() {
			return size;
		}

		private static String getSingleRowKeyColumnNotContainedInAssociationKey(AssociationKey key) {
			String nonKeyColumn = null;

			for ( String column : key.getRowKeyColumnNames() ) {
				if ( !key.isKeyColumn( column ) ) {
					if ( nonKeyColumn != null ) {
						// more than one column which is not contained in the association key; thus rows of this
						// association will be represented by DBObjects rather than single String, int etc. objects
						return null;
					}
					else {
						nonKeyColumn = column;
					}
				}
			}

			return nonKeyColumn;
		}
	}
}
