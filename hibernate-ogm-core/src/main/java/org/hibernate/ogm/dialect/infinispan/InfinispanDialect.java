/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.infinispan;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.atomic.FineGrainedAtomicMap;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.ogm.datastore.impl.EmptyTupleSnapshot;
import org.hibernate.ogm.datastore.impl.MapBasedTupleSnapshot;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationOperation;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.persister.entity.Lockable;

/**
 * @author Emmanuel Bernard
 */
public class InfinispanDialect implements GridDialect {

	private final InfinispanDatastoreProvider provider;

	public InfinispanDialect(InfinispanDatastoreProvider provider) {
		this.provider = provider;
	}

	/**
	 * Get a strategy instance which knows how to acquire a database-level lock
	 * of the specified mode for this dialect.
	 *
	 * @param lockable The persister for the entity to be locked.
	 * @param lockMode The type of lock to be acquired.
	 * @return The appropriate locking strategy.
	 * @since 3.2
	 */
	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		if ( lockMode==LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode==LockMode.PESSIMISTIC_WRITE ) {
			return new InfinispanPessimisticWriteLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode==LockMode.PESSIMISTIC_READ ) {
			//TODO find a more efficient pessimistic read
			return new InfinispanPessimisticWriteLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode==LockMode.OPTIMISTIC ) {
			return new OptimisticLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode==LockMode.OPTIMISTIC_FORCE_INCREMENT ) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		return new SelectLockingStrategy( lockable, lockMode );
	}

	@Override
	public Tuple getTuple(EntityKey key, Cache<EntityKey, Map<String, Object>> cache) {
		FineGrainedAtomicMap<String,Object> atomicMap = AtomicMapLookup.getFineGrainedAtomicMap( cache, key, false );
		if (atomicMap == null) {
			return null;
		}
		else {
			return new Tuple( new InfinispanTupleSnapshot( atomicMap ) );
		}
	}

	@Override
	public Tuple createTuple(EntityKey key, Cache<EntityKey, Map<String, Object>> cache) {
		//TODO we don't verify that it does not yet exist assuming that this ahs been done before by the calling code
		//should we improve?
		FineGrainedAtomicMap<String,Object> atomicMap =  AtomicMapLookup.getFineGrainedAtomicMap( cache, key, true );
		return new Tuple( new InfinispanTupleSnapshot( atomicMap ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, Cache<EntityKey, Map<String, Object>> cache) {
		Map<String,Object> atomicMap = ( (InfinispanTupleSnapshot) tuple.getSnapshot() ).getAtomicMap();
		applyTupleOpsOnMap( tuple, atomicMap );
	}

	private void applyTupleOpsOnMap(Tuple tuple, Map<String, Object> map) {
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

	@Override
	public void removeTuple(EntityKey key, Cache<EntityKey, Map<String, Object>> cache) {
		AtomicMapLookup.removeAtomicMap( cache, key );
	}

	@Override
	public Association getAssociation(AssociationKey key, Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache) {
		Map<RowKey, Map<String, Object>> atomicMap = AtomicMapLookup.getFineGrainedAtomicMap( cache, key, false );
		return atomicMap == null ? null : new Association( new InfinispanAssociationSnapshot( atomicMap ) );
	}

	@Override
	public Association createAssociation(AssociationKey key, Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache) {
		//TODO we don't verify that it does not yet exist assuming that this ahs been done before by the calling code
		//should we improve?
		Map<RowKey, Map<String, Object>> atomicMap =  AtomicMapLookup.getFineGrainedAtomicMap( cache, key, true );
		return new Association( new InfinispanAssociationSnapshot( atomicMap ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key, Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache) {
		Map<RowKey, Map<String, Object>> atomicMap = ( (InfinispanAssociationSnapshot) association.getSnapshot() ).getAtomicMap();
		for( AssociationOperation action : association.getOperations() ) {
			switch ( action.getType() ) {
				case CLEAR:
					atomicMap.clear();
				case PUT_NULL:
				case PUT:
					atomicMap.put( action.getKey(), tupleToMap( action.getValue() ) );
					break;
				case REMOVE:
					atomicMap.remove( action.getKey() );
					break;
			}
		}
	}

	Map<String, Object> tupleToMap(Tuple tuple) {
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
			//loaded assoc tuples are made of MapBasedTupleSnapshot
			snapshot = ( ( MapBasedTupleSnapshot) snapshotInstance ).getMap();
		}
		Map<String, Object> map = new HashMap<String, Object>( snapshot );
		applyTupleOpsOnMap( tuple, map );
		return map;
	}

	@Override
	public void removeAssociation(AssociationKey key, Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache) {
		AtomicMapLookup.removeAtomicMap( cache, key );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey, Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache) {
		return new Tuple( EmptyTupleSnapshot.SINGLETON );
	}
}
