/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protostream;

import java.util.Map;
import java.util.Objects;

import org.hibernate.AssertionFailure;
import org.hibernate.ogm.datastore.infinispanremote.impl.VersionedTuple;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;

public final class ProtostreamPayload {

	//One and only one of the following fields will be initialized:
	private final MapTupleSnapshot loadedSnapshot;
	private final Tuple tuple;

	public ProtostreamPayload(MapTupleSnapshot loadedSnapshot) {
		this.loadedSnapshot = Objects.requireNonNull( loadedSnapshot );
		this.tuple = null;
	}

	public ProtostreamPayload(Tuple tuple) {
		this.tuple = Objects.requireNonNull( tuple );
		this.loadedSnapshot = null;
	}

	public Tuple toTuple(SnapshotType snapshotType) {
		if ( loadedSnapshot != null ) {
			return new VersionedTuple( loadedSnapshot, snapshotType );
		}
		else {
			tuple.setSnapshotType( snapshotType );
			return tuple;
		}
	}

	public VersionedTuple toVersionedTuple(SnapshotType snapshotType) {
		if ( loadedSnapshot != null ) {
			return new VersionedTuple( loadedSnapshot, snapshotType );
		}
		else if ( tuple instanceof VersionedTuple ) {
			VersionedTuple vt = (VersionedTuple) tuple;
			vt.setSnapshotType( snapshotType );
			return vt;
		}
		else {
			throw new AssertionFailure( "toVersionedTuple() can only be used on just loaded instances" );
		}
	}

	public Object getColumnValue(String columnName) {
		if ( tuple != null ) {
			return tuple.get( columnName );
		}
		else {
			return loadedSnapshot.get( columnName );
		}
	}

	public Map<String, Object> toMap() {
		if ( loadedSnapshot != null ) {
			return loadedSnapshot.getMap();
		}
		else {
			MapTupleSnapshot tupleSnapshot = (MapTupleSnapshot) tuple.getSnapshot();
			return tupleSnapshot.getMap();
		}
	}

	public RowKey asRowKey(AssociationKey key) {
		String[] columnNames = key.getMetadata().getRowKeyColumnNames();
		Object[] columnValues = new Object[columnNames.length];
		for ( int i = 0; i < columnNames.length; i++ ) {
			String columnName = columnNames[i];
			columnValues[i] = getColumnValue( columnName );
		}
		return new RowKey( columnNames, columnValues );
	}

}
