/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.dialect.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.RowKey;

/**
 * An {@link AssociationSnapshot} based on a serializable map.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 * @author Gunnar Morling
 */
public final class SerializableMapAssociationSnapshot implements AssociationSnapshot {

	private final Map<SerializableKey, Map<String, Object>> associationMap;

	public SerializableMapAssociationSnapshot(Map<SerializableKey, Map<String, Object>> associationMap) {
		this.associationMap = associationMap;
	}

	@Override
	public Tuple get(RowKey column) {
		Map<String, Object> rawResult = associationMap.get( new SerializableKey( column ) );
		return rawResult != null ? new Tuple( new MapTupleSnapshot( rawResult ) ) : null;
	}

	@Override
	public boolean containsKey(RowKey column) {
		return associationMap.containsKey( new SerializableKey( column ) );
	}

	@Override
	public int size() {
		return associationMap.size();
	}

	@Override
	public Set<RowKey> getRowKeys() {
		Set<RowKey> rowKeys = new HashSet<RowKey>( associationMap.size() );

		for ( SerializableKey key : associationMap.keySet() ) {
			rowKeys.add( new RowKey( key.getTable(), key.getColumnNames(), key.getColumnValues(), null ) );
		}

		return rowKeys;
	}

	public Map<SerializableKey, Map<String, Object>> getUnderlyingMap() {
		return associationMap;
	}
}
