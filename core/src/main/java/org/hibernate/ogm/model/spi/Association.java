/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.spi;

import static org.hibernate.ogm.model.spi.AssociationOperationType.PUT;
import static org.hibernate.ogm.model.spi.AssociationOperationType.REMOVE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.impl.EmptyAssociationSnapshot;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.util.impl.Contracts;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * Represents an association (think of it as a set of rows, each representing a specific link).
 * <p>
 * An association accepts a {@link AssociationSnapshot} which is a read-only state of the association when read from the
 * database or freshly created.
 * <p>
 * An association collects changes applied to it. These changes are represented by a list of
 * {@link AssociationOperation}. It is intended that {@link GridDialect}s retrieve these actions and apply them to the
 * datastore. The list of changes is computed against the snapshot.
 * <p>
 * Note that the {@link Tuple}s representing association rows always also contain the columns of their {@link RowKey}.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
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

	/**
	 * Returns the association row with the given key.
	 *
	 * @param key the key of the row to return.
	 * @return the association row with the given key or {@code null} if no row with that key is contained in this
	 * association
	 */
	public Tuple get(RowKey key) {
		AssociationOperation result = currentState.get( key );
		if ( result == null ) {
			return cleared ? null : snapshot.get( key );
		}
		else if ( result.getType() == REMOVE ) {
			return null;
		}
		return result.getValue();
	}

	/**
	 * Adds the given row to this association, using the given row key.
	 * The row must not be null, use the {@link org.hibernate.ogm.model.spi.Association#remove(org.hibernate.ogm.model.key.spi.RowKey)}
	 * operation instead.
	 *
	 * @param key the key to store the row under
	 * @param value the association row to store
	 */
	public void put(RowKey key, Tuple value) {
		// instead of setting it to null, core must use remove
		Contracts.assertNotNull( value, "association.put value" );
		currentState.put( key, new AssociationOperation( key, value, PUT )  );
	}

	/**
	 * Removes the row with the specified key from this association.
	 *
	 * @param key the key of the association row to remove
	 */
	public void remove(RowKey key) {
		currentState.put( key, new AssociationOperation( key, null, REMOVE ) );
	}

	/**
	 * Return the list of actions on the tuple. Operations are inherently deduplicated, i.e. there will be at most one
	 * operation for a specific row key.
	 * <p>
	 * Note that the global CLEAR operation is put at the top of the list.
	 *
	 * @return the operations to execute on the association, the global CLEAR operation is put at the top of the list
	 */
	public List<AssociationOperation> getOperations() {
		List<AssociationOperation> result = new ArrayList<AssociationOperation>( currentState.size() + 1 );
		if (cleared) {
			result.add( new AssociationOperation( null, null, AssociationOperationType.CLEAR ) );
		}
		result.addAll( currentState.values() );
		return result;
	}

	/**
	 * Returns the snapshot upon which this association is based, i.e. its original state when loaded from the datastore
	 * or newly created.
	 *
	 * @return the snapshot upon which this association is based
	 */
	public AssociationSnapshot getSnapshot() {
		return snapshot;
	}

	/**
	 * Whether this association contains no rows.
	 *
	 * @return {@code true} if this association contains no rows, {@code false} otherwise
	 */
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

	/**
	 * Returns the number of rows within this association.
	 *
	 * @return the number of rows within this association
	 */
	public int size() {
		int size = cleared ? 0 : snapshot.size();
		for ( Map.Entry<RowKey,AssociationOperation> op : currentState.entrySet() ) {
			switch ( op.getValue().getType() ) {
				case PUT:
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

	/**
	 * Returns all keys of all rows contained within this association.
	 *
	 * @return all keys of all rows contained within this association
	 */
	public Iterable<RowKey> getKeys() {
		if ( cleared ) {
			return Collections.emptyList();
		}
		else if ( currentState.isEmpty() ) {
			return snapshot.getRowKeys();
		}
		else {
			// It may be a bit too large in case of removals, but that's fine for now
			Set<RowKey> keys = new HashSet<RowKey>( snapshot.size() + currentState.size() );
			for ( RowKey rowKey : snapshot.getRowKeys() ) {
				keys.add( rowKey );
			}

			for ( Map.Entry<RowKey,AssociationOperation> op : currentState.entrySet() ) {
				switch ( op.getValue().getType() ) {
					case PUT:
						keys.add( op.getKey() );
						break;
					case REMOVE:
						keys.remove( op.getKey() );
						break;
				}
			}

			return keys;
		}
	}

	/**
	 * Removes all rows from this association.
	 */
	public void clear() {
		cleared = true;
		currentState.clear();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( "Association[" ).append( StringHelper.lineSeparator() );

		Iterator<RowKey> rowKeys = getKeys().iterator();

		while ( rowKeys.hasNext() ) {
			RowKey rowKey = rowKeys.next();
			sb.append( "  " ).append( rowKey ).append( "=" ).append( get( rowKey ) );

			if ( rowKeys.hasNext() ) {
				sb.append( "," ).append( StringHelper.lineSeparator() );
			}
		}

		sb.append( StringHelper.lineSeparator() ).append( "]" );
		return sb.toString();
	}
}
