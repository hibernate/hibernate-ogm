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
package org.hibernate.ogm.datastore.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.map.impl.MapAssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationOperation;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public final class MapHelpers {

	private MapHelpers() {
		// not meant to be instantiated
	}

	public static void applyTupleOpsOnMap(Tuple tuple, Map<String, Object> map) {
		for( TupleOperation action : tuple.getOperations() ) {
			switch ( action.getType() ) {
				case PUT_NULL:
				case PUT:
					map.put( action.getColumn(), action.getValue() );
					break;
				case REMOVE:
					map.remove( action.getColumn() );
					break;
			}
		}
	}

	public static Map<String, Object> tupleToMap(Tuple tuple) {
		if (tuple == null) {
			return null;
		}
		Map<String, Object> snapshot;
		TupleSnapshot snapshotInstance = tuple.getSnapshot();
		if ( snapshotInstance == EmptyTupleSnapshot.SINGLETON ) {
			//new assoc tuples are made of EmptyTupleSnapshot
			snapshot = Collections.EMPTY_MAP;
		}
		else {
			//loaded assoc tuples are made of MapTupleSnapshot
			snapshot = ( (MapTupleSnapshot) snapshotInstance ).getMap();
		}
		Map<String, Object> map = new HashMap<String, Object>( snapshot );
		MapHelpers.applyTupleOpsOnMap( tuple, map );
		return map;
	}

	public static void updateAssociation(Association association, AssociationKey key) {
		Map<RowKey, Map<String, Object>> atomicMap = ( (MapAssociationSnapshot) association.getSnapshot() ).getUnderlyingMap();
		for( AssociationOperation action : association.getOperations() ) {
			switch ( action.getType() ) {
				case CLEAR:
					atomicMap.clear();
				case PUT_NULL:
				case PUT:
					atomicMap.put( action.getKey(), MapHelpers.tupleToMap( action.getValue() ) );
					break;
				case REMOVE:
					atomicMap.remove( action.getKey() );
					break;
			}
		}
	}

}
