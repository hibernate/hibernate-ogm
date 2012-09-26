/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.ehcache;

import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider;
import org.hibernate.ogm.datastore.impl.EmptyTupleSnapshot;
import org.hibernate.ogm.datastore.impl.MapHelpers;
import org.hibernate.ogm.datastore.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.map.impl.MapAssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.DefaultDatastoreNames;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

/**
 * @author Alex Snaps
 */
public class EhcacheDialect implements GridDialect {

	EhcacheDatastoreProvider datastoreProvider;

	public EhcacheDialect(EhcacheDatastoreProvider datastoreProvider) {
		this.datastoreProvider = datastoreProvider;
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		if ( lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
//		else if ( lockMode==LockMode.PESSIMISTIC_WRITE ) {
//			return new EhcachePessimisticWriteLockingStrategy( lockable, lockMode );
//		}
//		else if ( lockMode==LockMode.PESSIMISTIC_READ ) {
			//TODO find a more efficient pessimistic read
//			return new EhcachePessimisticWriteLockingStrategy( lockable, lockMode );
//		}
		else if ( lockMode == LockMode.OPTIMISTIC ) {
			return new OptimisticLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT ) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		throw new UnsupportedOperationException( "LockMode " + lockMode + " is not supported by the Ehcache GridDialect" );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		final Cache entityCache = getEntityCache();
		final Element element = entityCache.get( key );
		if ( element != null ) {
			return new Tuple( new MapTupleSnapshot( (Map<String, Object>) element.getValue() ) );
		}
		else {
			return null;
		}
	}

	@Override
	public Tuple createTuple(EntityKey key) {
		final Cache entityCache = getEntityCache();
		final HashMap<String, Object> tuple = new HashMap<String, Object>();
		entityCache.put( new Element( key, tuple ) );
		return new Tuple( new MapTupleSnapshot( tuple ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key) {
		Map<String, Object> entityRecord = ( (MapTupleSnapshot) tuple.getSnapshot() ).getMap();
		MapHelpers.applyTupleOpsOnMap( tuple, entityRecord );
	}

	@Override
	public void removeTuple(EntityKey key) {
		getEntityCache().remove( key );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		final Cache associationCache = getAssociationCache();
		final Element element = associationCache.get( key );
		if ( element == null ) {
			return null;
		}
		else {
			return new Association( new MapAssociationSnapshot( (Map) element.getValue() ) );
		}
	}

	@Override
	public Association createAssociation(AssociationKey key) {
		final Cache associationCache = getAssociationCache();
		Map<RowKey, Map<String, Object>> association = new HashMap<RowKey, Map<String, Object>>();
		associationCache.put( new Element( key, association ) );
		return new Association( new MapAssociationSnapshot( association ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key) {
		MapHelpers.updateAssociation( association, key );
	}

	@Override
	public void removeAssociation(AssociationKey key) {
		getAssociationCache().remove( key );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return new Tuple( EmptyTupleSnapshot.SINGLETON );
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		final Cache cache = getIdentifierCache();
		Element previousValue = cache.get( key );
		if ( previousValue == null ) {
			previousValue = cache.putIfAbsent( new Element( key, initialValue ) );
		}
		if ( previousValue != null ) {
			while ( !cache.replace( previousValue,
					new Element( key, ( (Integer) previousValue.getValue() ) + increment ) ) ) {
				previousValue = cache.get( key );
			}
			value.initialize( ( (Integer) previousValue.getValue() ) + increment );
		}
		else {
			value.initialize( initialValue );
		}
	}

	@Override
	public GridType overrideType(Type type) {
		return null;
	}

	private Cache getIdentifierCache() {
		return datastoreProvider.getCacheManager().getCache( DefaultDatastoreNames.IDENTIFIER_STORE );
	}

	private Cache getEntityCache() {
		return datastoreProvider.getCacheManager().getCache( DefaultDatastoreNames.ENTITY_STORE );
	}

	private Cache getAssociationCache() {
		return datastoreProvider.getCacheManager().getCache( DefaultDatastoreNames.ASSOCIATION_STORE );
	}
}
