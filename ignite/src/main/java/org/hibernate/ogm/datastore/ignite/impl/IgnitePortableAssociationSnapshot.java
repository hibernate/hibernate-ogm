/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.ignite.binary.BinaryObject;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;

/**
 * @author Victor Kadachigov
 */
public class IgnitePortableAssociationSnapshot implements AssociationSnapshot {

	private final Map<RowKey, BinaryObject> associationMap;

	public IgnitePortableAssociationSnapshot(String rowKeyIndexColumnNames[]) {
		if ( rowKeyIndexColumnNames != null && rowKeyIndexColumnNames.length > 0 ) {
			this.associationMap = new TreeMap<RowKey, BinaryObject>( new RawKeyComparator( rowKeyIndexColumnNames ) );
		}
		else {
			this.associationMap = new HashMap<RowKey, BinaryObject>();
		}
	}

	public IgnitePortableAssociationSnapshot(Map<RowKey, BinaryObject> associationMap, String rowKeyIndexColumnNames[]) {
		this( rowKeyIndexColumnNames );
		this.associationMap.putAll( associationMap );
	}

	@Override
	public Tuple get(RowKey rowKey) {
		BinaryObject object = associationMap.get( rowKey );
		return object != null ? new Tuple( new IgnitePortableTupleSnapshot( object ), SnapshotType.UPDATE ) : null;
	}

	@Override
	public boolean containsKey(RowKey rowKey) {
		return associationMap.containsKey( rowKey );
	}

	@Override
	public int size() {
		return associationMap.size();
	}

	@Override
	public Iterable<RowKey> getRowKeys() {
		return associationMap.keySet();
	}

	public BinaryObject getBinary(RowKey rowKey) {
		return associationMap.get( rowKey );
	}

	private class RawKeyComparator implements Comparator<RowKey> {

		private final String sortFields[];

		public RawKeyComparator(String[] sortFields) {
			this.sortFields = sortFields != null ? sortFields : new String[0];
		}

		@Override
		public int compare(RowKey key1, RowKey key2) {
			CompareToBuilder builder = new CompareToBuilder();
			for ( String name : sortFields ) {
				builder.append( key1.getColumnValue( name ), key2.getColumnValue( name ) );
			}
			return builder.toComparison();
		}
	}
}
