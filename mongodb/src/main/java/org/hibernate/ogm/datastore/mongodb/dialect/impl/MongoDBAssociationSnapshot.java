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

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * @author Alan Fitton <alan at eth0.org.uk>
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MongoDBAssociationSnapshot implements AssociationSnapshot {

	private final Map<RowKey, DBObject> map;
	private final DBObject dbObject;

	public MongoDBAssociationSnapshot(DBObject document, AssociationKey key, AssociationStorageStrategy storageStrategy) {
		this.dbObject = document;

		Collection<DBObject> associationRows = getRows( document, key, storageStrategy );
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

	@SuppressWarnings("unchecked")
	private static Collection<DBObject> getRows(DBObject document, AssociationKey associationKey, AssociationStorageStrategy storageStrategy) {
		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			return getAssociationFieldOrNull( associationKey, document );
		}
		else {
			return (Collection<DBObject>) document.get( MongoDBDialect.ROWS_FIELDNAME );
		}
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
}
