/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import static org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoHelpers.getAssociationFieldOrNull;
import static org.hibernate.ogm.util.impl.CollectionHelper.newHashMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot.SnapshotType;
import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.grid.impl.RowKeyBuilder;
import org.hibernate.ogm.util.impl.Contracts;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * An association snapshot based on a {@link DBObject} retrieved from MongoDB.
 *
 * @author Alan Fitton <alan at eth0.org.uk>
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Gunnar Morling
 */
public class MongoDBAssociationSnapshot implements AssociationSnapshot {

	private final Map<RowKey, DBObject> map;
	private final DBObject dbObject;

	public MongoDBAssociationSnapshot(DBObject document, AssociationKey key, AssociationStorageStrategy storageStrategy) {
		this.dbObject = document;
		AssociationRows associationRows = getRows( document, key, storageStrategy );
		this.map = new LinkedHashMap<RowKey, DBObject>( associationRows.size() );

		for ( DBObject row : associationRows ) {
			RowKey rowKey = new RowKeyBuilder()
					.tableName( key.getTable() )
					.addColumns( key.getRowKeyColumnNames() )
					.values( getRowKeyColumnValues( row, key ) )
					.build();

			map.put( rowKey, row );
		}
	}

	/**
	 * Returns the values of the row key of the given association; columns present in the given association key will be
	 * obtained from there, all other columns from the given {@link DBObject}.
	 */
	private static Map<String, Object> getRowKeyColumnValues(DBObject row, AssociationKey key) {
		Map<String, Object> rowKeyColumnValues = newHashMap( key.getRowKeyColumnNames().length );

		for ( String rowKeyColumnName : key.getRowKeyColumnNames() ) {
			rowKeyColumnValues.put(
					rowKeyColumnName,
					key.isKeyColumn( rowKeyColumnName ) ? key.getColumnValue( rowKeyColumnName ) : row.get( rowKeyColumnName )
			);
		}

		return rowKeyColumnValues;
	}

	@Override
	public Tuple get(RowKey column) {
		DBObject row = this.map.get( column );
		return row == null ? null : new Tuple( new MongoDBTupleSnapshot( row, column, SnapshotType.SELECT ) );
	}

	//not for embedded
	public DBObject getQueryObject() {
		DBObject query = new BasicDBObject();
		query.put( MongoDBDialect.ID_FIELDNAME, dbObject.get( MongoDBDialect.ID_FIELDNAME ) );
		return query;
	}

	@Override
	public boolean containsKey(RowKey column) {
		return map.containsKey( column );
	}

	@Override
	public int size() {
		return map.size();
	}

	private static AssociationRows getRows(DBObject document, AssociationKey associationKey, AssociationStorageStrategy storageStrategy) {
		Collection<?> rows;

		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			rows = getAssociationFieldOrNull( associationKey, document );
		}
		else {
			rows = (Collection<?>) document.get( MongoDBDialect.ROWS_FIELDNAME );
		}

		return new AssociationRows( rows, associationKey );
	}

	@Override
	public Set<RowKey> getRowKeys() {
		return map.keySet();
	}

	// TODO This only is used for tests; Can we get rid of it?
	public DBObject getDBObject() {
		return this.dbObject;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "MongoDBAssociationSnapshot(" );
		sb.append( map.size() );
		sb.append( ") RowKey entries)." );
		return sb.toString();
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
	private static class AssociationRows implements Iterable<DBObject> {

		private final Iterator<?> wrapped;
		private final int size;
		private final String fieldName;

		public AssociationRows(Collection<?> wrapped, AssociationKey key) {
			this.wrapped = wrapped.iterator();
			this.size = wrapped.size();
			this.fieldName = getSingleRowKeyColumnNotContainedInAssociationKey( key );
		}

		@Override
		public Iterator<DBObject> iterator() {
			return new Iterator<DBObject>() {

				@Override
				public DBObject next() {
					Object next = wrapped.next();
					if ( next instanceof DBObject ) {
						return (DBObject) next;
					}
					else {
						Contracts.assertNotNull( fieldName, "fieldName" );
						return new BasicDBObject( fieldName, next );
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
