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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.grid.RowKey;

import static org.hibernate.ogm.datastore.spi.AssociationOperationType.*;

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

	public Association(AssociationSnapshot snapshot) {
		this.snapshot = snapshot;
	}

	public Tuple get(RowKey key) {
		AssociationOperation result = currentState.get( key );
		if ( result == null ) {
			return snapshot.get( key );
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
	 */
	public Set<AssociationOperation> getOperations() {
		return new HashSet<AssociationOperation>( currentState.values() );
	}

	public AssociationSnapshot getSnapshot() {
		return snapshot;
	}
}
