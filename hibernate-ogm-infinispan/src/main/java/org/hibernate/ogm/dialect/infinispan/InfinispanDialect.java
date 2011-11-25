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

import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ASSOCIATION_STORE;
import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ENTITY_STORE;
import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.IDENTIFIER_STORE;

import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.impl.EmptyTupleSnapshot;
import org.hibernate.ogm.datastore.impl.MapHelpers;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.mapbased.impl.MapAssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.persister.entity.Lockable;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.atomic.FineGrainedAtomicMap;
import org.infinispan.context.Flag;

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
		throw new UnsupportedOperationException( "LockMode " + lockMode + " is not supported by the Infinispan GridDialect" );
	}

	@Override
	public Tuple getTuple(EntityKey key) {
		Cache<EntityKey, Map<String, Object>> cache = provider.getCache(ENTITY_STORE);
		FineGrainedAtomicMap<String,Object> atomicMap = AtomicMapLookup.getFineGrainedAtomicMap( cache, key, false );
		if (atomicMap == null) {
			return null;
		}
		else {
			return new Tuple( new InfinispanTupleSnapshot( atomicMap ) );
		}
	}

	@Override
	public Tuple createTuple(EntityKey key) {
		//TODO we don't verify that it does not yet exist assuming that this has been done before by the calling code
		//should we improve?
		Cache<EntityKey, Map<String, Object>> cache = provider.getCache(ENTITY_STORE);
		FineGrainedAtomicMap<String,Object> atomicMap =  AtomicMapLookup.getFineGrainedAtomicMap( cache, key, true );
		return new Tuple( new InfinispanTupleSnapshot( atomicMap ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key) {
		Map<String,Object> atomicMap = ( (InfinispanTupleSnapshot) tuple.getSnapshot() ).getAtomicMap();
		MapHelpers.applyTupleOpsOnMap( tuple, atomicMap );
	}

	@Override
	public void removeTuple(EntityKey key) {
		Cache<EntityKey, Map<String, Object>> cache = provider.getCache(ENTITY_STORE);
		AtomicMapLookup.removeAtomicMap( cache, key );
	}

	@Override
	public Association getAssociation(AssociationKey key) {
		Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache = provider.getCache(ASSOCIATION_STORE);
		Map<RowKey, Map<String, Object>> atomicMap = AtomicMapLookup.getFineGrainedAtomicMap( cache, key, false );
		return atomicMap == null ? null : new Association( new MapAssociationSnapshot( atomicMap ) );
	}

	@Override
	public Association createAssociation(AssociationKey key) {
		//TODO we don't verify that it does not yet exist assuming that this ahs been done before by the calling code
		//should we improve?
		Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache = provider.getCache(ASSOCIATION_STORE);
		Map<RowKey, Map<String, Object>> atomicMap =  AtomicMapLookup.getFineGrainedAtomicMap( cache, key, true );
		return new Association( new MapAssociationSnapshot( atomicMap ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key) {
		MapHelpers.updateAssociation( association, key );
	}

	@Override
	public void removeAssociation(AssociationKey key) {
		Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache = provider.getCache(ASSOCIATION_STORE);
		AtomicMapLookup.removeAtomicMap( cache, key );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return new Tuple( EmptyTupleSnapshot.SINGLETON );
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		final AdvancedCache<RowKey, Object> identifierCache = provider.getCache(IDENTIFIER_STORE).getAdvancedCache();
		boolean done = false;
		do {
			//read value
			//skip locking proposed by Sanne
			Object valueFromDb = identifierCache.withFlags( Flag.SKIP_LOCKING ).get( key );
			if ( valueFromDb == null ) {
				//if not there, insert initial value
				value.initialize( initialValue );
				//TODO should we use GridTypes here?
				valueFromDb = new Long( value.makeValue().longValue() );
				final Object oldValue = identifierCache.putIfAbsent( key, valueFromDb );
				//check in case somebody has inserted it behind our back
				if ( oldValue != null ) {
					value.initialize( ( (Number) oldValue ).longValue() );
					valueFromDb = oldValue;
				}
			}
			else {
				//read the value from the table
				value.initialize( ( ( Number ) valueFromDb ).longValue() );
			}

			//update value
			final IntegralDataTypeHolder updateValue = value.copy();
			//increment value
			updateValue.add( increment );
			//TODO should we use GridTypes here?
			final Object newValueFromDb = updateValue.makeValue().longValue();
			done = identifierCache.replace( key, valueFromDb, newValueFromDb );
		}
		while ( !done );
	}
}
