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

import org.apache.ignite.binary.BinaryObject;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationSnapshot;
import org.hibernate.ogm.model.spi.Tuple;

public class IgnitePortableAssociationSnapshot implements AssociationSnapshot {

	private final Map<RowKey, BinaryObject> associationMap;
	private final String rowKeyIndexColumnNames[];

	public IgnitePortableAssociationSnapshot(String rowKeyIndexColumnNames[]) {
		this.rowKeyIndexColumnNames = rowKeyIndexColumnNames;
		this.associationMap = new HashMap<>();
	}

	public IgnitePortableAssociationSnapshot(Map<RowKey, BinaryObject> associationMap, String rowKeyIndexColumnNames[]) {
		this.rowKeyIndexColumnNames = rowKeyIndexColumnNames;
		Comparator<RowKey> comparator = createMapComparator();
		this.associationMap = comparator != null ? new TreeMap<RowKey, BinaryObject>( comparator ) : new HashMap<RowKey, BinaryObject>();
		this.associationMap.putAll( associationMap );
	}

	private Comparator<RowKey> createMapComparator() {
		Comparator<RowKey> result = null;
		if ( rowKeyIndexColumnNames != null && rowKeyIndexColumnNames.length > 0 ) {
		}
		return result;
	}

	@Override
	public Tuple get(RowKey rowKey) {
		BinaryObject object = associationMap.get( rowKey );
		return object != null ? new Tuple( new IgnitePortableTupleSnapshot( object ) ) : null;
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
}
