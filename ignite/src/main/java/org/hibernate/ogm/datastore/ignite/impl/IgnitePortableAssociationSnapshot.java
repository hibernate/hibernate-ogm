/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.ignite.binary.BinaryObject;
import org.hibernate.ogm.model.key.spi.AssociationKey;
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
public class IgnitePortableAssociationSnapshot implements AssociationSnapshot { // extends AssociationRows {

	private final Map<RowKey, IgnitePortableTupleSnapshot> rows;

	public IgnitePortableAssociationSnapshot(AssociationKey associationKey) {
//		super( associationKey, Collections.emptySet(), IgniteAssociationRowFactory.INSTANCE );
		rows = Collections.emptyMap();
	}

	public IgnitePortableAssociationSnapshot(AssociationKey associationKey, Map<Object, BinaryObject> associationMap) {
//		super( associationKey, associationMap.entrySet(), IgniteAssociationRowFactory.INSTANCE );
		this.rows = CollectionHelper.newHashMap( associationMap.size() );
		for ( Map.Entry<Object, BinaryObject> entry : associationMap.entrySet() ) {
			IgnitePortableTupleSnapshot snapshot = new IgnitePortableTupleSnapshot( entry.getKey(), entry.getValue(), associationKey.getMetadata().getAssociatedEntityKeyMetadata().getEntityKeyMetadata() );
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
		IgnitePortableTupleSnapshot row = rows.get( rowKey );
		return row != null ? row.getCacheKey() : null;
	}

	/**
	 * @return value object in underlaying cache
	 */
	public BinaryObject getCacheValue(RowKey rowKey) {
		IgnitePortableTupleSnapshot row = rows.get( rowKey );
		return row != null ? row.getCacheValue() : null;
	}
}
