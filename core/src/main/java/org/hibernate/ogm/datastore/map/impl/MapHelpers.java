/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.map.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationOperation;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.TupleOperation;
import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
public final class MapHelpers {

	private MapHelpers() {
		// not meant to be instantiated
	}

	public static void applyTupleOpsOnMap(Tuple tuple, Map<String, Object> map) {
		for ( TupleOperation action : tuple.getOperations() ) {
			switch ( action.getType() ) {
				case PUT:
					map.put( action.getColumn(), action.getValue() );
					break;
				case REMOVE:
				case PUT_NULL:
					map.remove( action.getColumn() );
					break;
			}
		}
	}

	public static Map<String, Object> associationRowToMap(Tuple associationRow) {
		if (associationRow == null) {
			return null;
		}
		Map<String, Object> snapshot;
		TupleSnapshot snapshotInstance = associationRow.getSnapshot();
		if ( snapshotInstance.isEmpty() ) {
			//new assoc tuples are made of EmptyTupleSnapshot
			snapshot = Collections.emptyMap();
		}
		else {
			//loaded assoc tuples are made of MapTupleSnapshot
			snapshot = ( (MapTupleSnapshot) snapshotInstance ).getMap();
		}
		Map<String, Object> map = new HashMap<String, Object>( snapshot );
		MapHelpers.applyTupleOpsOnMap( associationRow, map );
		return map;
	}

	public static void updateAssociation(Association association) {
		Map<RowKey, Map<String, Object>> underlyingMap = ( (MapAssociationSnapshot) association.getSnapshot() ).getUnderlyingMap();
		for ( AssociationOperation action : association.getOperations() ) {
			switch ( action.getType() ) {
				case CLEAR:
					underlyingMap.clear();
					break;
				case PUT:
					underlyingMap.put( action.getKey(), MapHelpers.associationRowToMap( action.getValue() ) );
					break;
				case REMOVE:
					underlyingMap.remove( action.getKey() );
					break;
			}
		}
	}

}
