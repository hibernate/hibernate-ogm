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

import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.atomic.AtomicMapLookup;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.persister.entity.Lockable;

/**
 * @author Emmanuel Bernard
 */
public class InfinispanDialect implements GridDialect {

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
		if ( lockMode==LockMode.PESSIMISTIC_FORCE_INCREMENT) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.PESSIMISTIC_WRITE) {
			return new InfinispanPessimisticWriteLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.PESSIMISTIC_READ) {
			//TODO find a more efficient pessimistic read
			return new InfinispanPessimisticWriteLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.OPTIMISTIC) {
			return new OptimisticLockingStrategy( lockable, lockMode);
		}
		else if ( lockMode==LockMode.OPTIMISTIC_FORCE_INCREMENT) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode);
		}
		return new SelectLockingStrategy( lockable, lockMode );
	}

	@Override
	public Tuple getTuple(EntityKey key, Cache<EntityKey, Map<String, Object>> cache) {
		AtomicMap<String,Object> atomicMap = AtomicMapLookup.getAtomicMap( cache, key, false );
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
		Map<String,Object> atomicMap =  AtomicMapLookup.getAtomicMap( cache, key, true );
		return new Tuple( new InfinispanTupleSnapshot( atomicMap ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, Cache<EntityKey, Map<String, Object>> cache) {
		Map<String,Object> atomicMap = ( (InfinispanTupleSnapshot) tuple.getSnapshot() ).getAtomicMap();
		for( TupleOperation action : tuple.getOperations() ) {
			switch ( action.getType() ) {
				case PUT_NULL:
				case PUT:
					atomicMap.put( action.getColumn(), action.getValue() );
					break;
				case REMOVE:
					atomicMap.remove( action.getColumn() );
					break;
			}
		}
	}

	@Override
	public void removeTuple(EntityKey key, Cache<EntityKey, Map<String, Object>> cache) {
		AtomicMapLookup.removeAtomicMap( cache, key );
	}

	@Override
	public Map<RowKey, Map<String, Object>> getAssociation(AssociationKey key, Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache) {
		return AtomicMapLookup.getAtomicMap( cache, key, false );
	}

	@Override
	public Map<RowKey, Map<String, Object>> createAssociation(AssociationKey key, Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache) {
		//TODO we don't verify that it does not yet exist assuming that this ahs been done before by the calling code
		//should we improve?
		return AtomicMapLookup.getAtomicMap( cache, key, true );
	}

	@Override
	public void updateAssociation(Map<RowKey, Map<String, Object>> association, AssociationKey key, Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache) {
	}

	@Override
	public void removeAssociation(AssociationKey key, Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache) {
		AtomicMapLookup.removeAtomicMap( cache, key );
	}


}
