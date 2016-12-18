/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.ignite.binary.BinaryObject;
import org.hibernate.ogm.datastore.ignite.util.StringHelper;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 *
 * @author Victor Kadachigov
 */
public class IgniteEmbeddedAssociationSnapshot implements AssociationSnapshot {

	private final Map<RowKey, TupleSnapshot> rows;
	private final AssociationKeyMetadata associationMetadata;
	private final Tuple tuple;

	public IgniteEmbeddedAssociationSnapshot(AssociationKey associationKey, Tuple tuple) {
		this.associationMetadata = associationKey.getMetadata();
		this.tuple = tuple;
		BinaryObject obj = ( (IgniteTupleSnapshot) tuple.getSnapshot() ).getCacheValue();
		Object objects[] = obj != null ? (Object[]) obj.field( StringHelper.realColumnName( associationMetadata.getCollectionRole() ) ) : null;
		rows = new HashMap<>();
		if ( objects != null ) {
			String indexColumnName = IgniteAssociationSnapshot.findIndexColumnName( associationMetadata );
			String rowKeyColumnNames[] = new String[ associationMetadata.getRowKeyColumnNames().length ];
			for ( int i = 0; i < rowKeyColumnNames.length; i++ ) {
				rowKeyColumnNames[i] = StringHelper.stringAfterPoint( associationMetadata.getRowKeyColumnNames()[i] );
			}
			for ( int i = 0; i < objects.length; i++ ) {
				BinaryObject itemObject = (BinaryObject) objects[i];
				Object rowKeyColumnValues[] = new Object[rowKeyColumnNames.length];
				for ( int j = 0; j < rowKeyColumnNames.length; j++ ) {
					rowKeyColumnValues[j] = itemObject.field( rowKeyColumnNames[j] );
				}
				RowKey rowKey = new RowKey( associationMetadata.getRowKeyColumnNames(), rowKeyColumnValues );
				this.rows.put( rowKey, new IgniteTupleSnapshot( null, itemObject, associationMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata() ) );
			}
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

}
