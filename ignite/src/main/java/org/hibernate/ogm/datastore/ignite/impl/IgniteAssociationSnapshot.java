/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ignite.binary.BinaryObject;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.util.impl.CollectionHelper;

/**
 *
 * @author Victor Kadachigov
 */
public class IgniteAssociationSnapshot implements AssociationSnapshot {

	private final Map<RowKey, IgniteAssociationRowSnapshot> rows;

	public IgniteAssociationSnapshot(AssociationKey associationKey) {
		rows = new LinkedHashMap<>(  );
	}

	public IgniteAssociationSnapshot(AssociationKey associationKey, Map<Object, BinaryObject> associationMap) {
		this.rows = CollectionHelper.newHashMap( associationMap.size() );
		for ( Map.Entry<Object, BinaryObject> entry : associationMap.entrySet() ) {
			IgniteAssociationRowSnapshot snapshot = new IgniteAssociationRowSnapshot( entry.getKey(), entry.getValue(), associationKey.getMetadata() );
			String rowKeyColumnNames[] = associationKey.getMetadata().getRowKeyColumnNames();
			Object rowKeyColumnValues[] = new Object[rowKeyColumnNames.length];
			for ( int i = 0; i < rowKeyColumnNames.length; i++ ) {
				String columnName = rowKeyColumnNames[i];
				rowKeyColumnValues[i] = snapshot.get( columnName );
			}
			RowKey rowKey = new RowKey( rowKeyColumnNames, rowKeyColumnValues );
			this.rows.put( rowKey, snapshot );
		}
	}

	@Override
	public Tuple get(RowKey rowKey) {
		TupleSnapshot row = rows.get( rowKey );
		return row != null ? new Tuple( row, SnapshotType.UPDATE ) : null;
	}

	@Override
	public boolean containsKey(RowKey rowKey) {
		return rows.containsKey( rowKey );
	}

	@Override
	public Set<RowKey> getRowKeys() {
		return rows.keySet();
	}

	@Override
	public int size() {
		return rows.size();
	}

	/**
	 * @return key object in underlaying cache
	 */
	public Object getCacheKey(RowKey rowKey) {
		IgniteAssociationRowSnapshot row = rows.get( rowKey );
		return row != null ? row.getCacheKey() : null;
	}

	/**
	 * @return value object in underlaying cache
	 */
	public BinaryObject getCacheValue(RowKey rowKey) {
		IgniteAssociationRowSnapshot row = rows.get( rowKey );
		return row != null ? row.getCacheValue() : null;
	}

	/**
	 * @param associationMetadata
	 * @return true - is association through third table
	 */
	public static boolean isThirdTableAssociation(AssociationKeyMetadata associationMetadata) {
		return !associationMetadata.getTable().equals(
						associationMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata().getTable()
				);
	}
}
