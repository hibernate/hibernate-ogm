/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.spi;

import static org.hibernate.ogm.datastore.spi.AssociationOperationType.PUT;
import static org.hibernate.ogm.datastore.spi.AssociationOperationType.PUT_NULL;
import static org.hibernate.ogm.datastore.spi.AssociationOperationType.REMOVE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.grid.RowKey;

/**
 * Represents an Association (think of it as a set of rows)
 *
 * A Association accepts a AssociationShapshot which is a read-only state
 * of the association at creation time.
 *
 * An association collects changes applied to it. These changes are represented by a
 * list of AssociationOperation. It is intended that GridDialects retrieve to these actions and
 * reproduce them to the datastore. The list of changes is computed based off the snapshot.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class Association {
	private final AssociationSnapshot snapshot;
	private final Map<RowKey, AssociationOperation> currentState = new HashMap<RowKey, AssociationOperation>();
	private boolean cleared;

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
}
