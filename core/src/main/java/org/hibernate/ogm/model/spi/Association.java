/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.spi;

import static org.hibernate.ogm.model.spi.AssociationOperationType.PUT;
import static org.hibernate.ogm.model.spi.AssociationOperationType.PUT_NULL;
import static org.hibernate.ogm.model.spi.AssociationOperationType.REMOVE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.impl.EmptyAssociationSnapshot;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.RowKey;

/**
 * Represents an association (think of it as a set of rows, each representing a specific link).
 * <p>
 * An association accepts a {@link AssociationSnapshot} which is a read-only state of the association when read from the
 * database or freshly created.
 * <p>
 * An association collects changes applied to it. These changes are represented by a list of
 * {@link AssociationOperation}. It is intended that {@link GridDialect}s retrieve these actions and apply them to the
 * datastore. The list of changes is computed against the snapshot.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class Association {
	private final AssociationSnapshot snapshot;
	private final Map<RowKey, AssociationOperation> currentState = new HashMap<RowKey, AssociationOperation>();
	private boolean cleared;

	/**
	 * Creates a new association, based on an empty association snapshot.
	 */
	public Association() {
		this.snapshot = EmptyAssociationSnapshot.INSTANCE;
	}

	public Association(AssociationSnapshot snapshot) {
		this.snapshot = snapshot;
	}

	public Tuple get(RowKey key) {
		AssociationOperation result = currentState.get( key );
		if ( result == null ) {
			return cleared ? null : snapshot.get( key );
		}
		else if ( result.getType() == PUT_NULL || result.getType() == REMOVE ) {
			return null;
		}
		return result.getValue();
	}

	public void put(RowKey key, Tuple value) {
		if ( value == null ) {
			currentState.put( key, new AssociationOperation( key, null, PUT_NULL )  );
		}
		currentState.put( key, new AssociationOperation( key, value, PUT ) );
	}

	public void remove(RowKey key) {
		currentState.put( key, new AssociationOperation( key, null, REMOVE ) );
	}

	/**
	 * Return the list of actions on the tuple.
	 * Inherently deduplicate operations
	 *
	 * Note that the global CLEAR operation is put at the top of the list.
	 */
	public List<AssociationOperation> getOperations() {
		List<AssociationOperation> result = new ArrayList<AssociationOperation>(  );
		if (cleared) {
			result.add( new AssociationOperation( null, null, AssociationOperationType.CLEAR ) );
		}
		result.addAll( currentState.values() );
		return result;
	}

	public AssociationSnapshot getSnapshot() {
		return snapshot;
	}

	public boolean isEmpty() {
		int snapshotSize = cleared ? 0 : snapshot.size();
		//nothing in both
		if ( snapshotSize == 0 && currentState.isEmpty() ) {
			return true;
		}
		//snapshot bigger than changeset
		if ( snapshotSize > currentState.size() ) {
			return false;
		}
		return size() == 0;
	}

	public int size() {
		int size = cleared ? 0 : snapshot.size();
		for ( Map.Entry<RowKey,AssociationOperation> op : currentState.entrySet() ) {
			switch ( op.getValue().getType() ) {
				case PUT:
				case PUT_NULL:
					if ( cleared || !snapshot.containsKey( op.getKey() ) ) {
						size++;
					}
					break;
				case REMOVE:
					if ( !cleared && snapshot.containsKey( op.getKey() ) ) {
						size--;
					}
					break;
			}
		}
		return size;
	}

	public Set<RowKey> getKeys() {
		Set<RowKey> keys = new HashSet<RowKey>();
		if (!cleared) {
			keys.addAll( snapshot.getRowKeys() );
		}
		for ( Map.Entry<RowKey,AssociationOperation> op : currentState.entrySet() ) {
			switch ( op.getValue().getType() ) {
				case PUT:
				case PUT_NULL:
					keys.add( op.getKey() );
					break;
				case REMOVE:
					keys.remove( op.getKey() );
					break;
			}
		}
		return keys;
	}

	public void clear() {
		cleared = true;
		currentState.clear();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( "Association[\n");
		int i = 0;
		for ( RowKey rowKey : getKeys() ) {
			sb.append( "  " ).append( rowKey ).append( "=" ).append( get( rowKey ) );
			i++;
			if ( i < getKeys().size() ) {
				sb.append( ",\n" );
			}
		}

		sb.append( "\n]" );
		return sb.toString();
	}
}
