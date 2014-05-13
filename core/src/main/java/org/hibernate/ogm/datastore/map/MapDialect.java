/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.map;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.map.impl.MapAssociationSnapshot;
import org.hibernate.ogm.datastore.map.impl.MapDatastoreProvider;
import org.hibernate.ogm.datastore.map.impl.MapHelpers;
import org.hibernate.ogm.datastore.map.impl.MapPessimisticReadLockingStrategy;
import org.hibernate.ogm.datastore.map.impl.MapPessimisticWriteLockingStrategy;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.TupleIterator;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.query.NoOpParameterMetadataBuilder;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.type.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

/**
 * Grid dialect which uses a plain map for storing objects in memory. For testing purposes.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class MapDialect implements GridDialect {

	private final MapDatastoreProvider provider;

	public MapDialect(MapDatastoreProvider provider) {
		this.provider = provider;
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		if ( lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_WRITE ) {
			return new MapPessimisticWriteLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_READ ) {
			return new MapPessimisticReadLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC ) {
			return new OptimisticLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT ) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		return new MapPessimisticWriteLockingStrategy( lockable, lockMode );
	}


	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		Map<String, Object> entityMap = provider.getEntityTuple( key );
		if ( entityMap == null ) {
			return null;
		}
		else {
			return new Tuple( new MapTupleSnapshot( entityMap ) );
		}
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		HashMap<String,Object> tuple = new HashMap<String,Object>();
		provider.putEntity( key, tuple );
		return new Tuple( new MapTupleSnapshot( tuple ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		Map<String,Object> entityRecord = ( (MapTupleSnapshot) tuple.getSnapshot() ).getMap();
		MapHelpers.applyTupleOpsOnMap( tuple, entityRecord );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		provider.removeEntityTuple( key );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		Map<RowKey, Map<String, Object>> associationMap = provider.getAssociation( key );
		return associationMap == null ? null : new Association( new MapAssociationSnapshot( associationMap ) );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		Map<RowKey, Map<String, Object>> associationMap = new HashMap<RowKey, Map<String,Object>>();
		provider.putAssociation( key, associationMap );
		return new Association( new MapAssociationSnapshot( associationMap ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext) {
		MapHelpers.updateAssociation( association, key );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		provider.removeAssociation( key );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return new Tuple();
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
		return false;
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		int nextValue = provider.getSharedAtomicInteger( key, initialValue, increment );
		value.initialize( nextValue );
	}

	@Override
	public GridType overrideType(Type type) {
		return null;
	}

	@Override
	public void forEachTuple(Consumer consumer, EntityKeyMetadata... metadatas) {
		Map<EntityKey, Map<String, Object>> entityMap = provider.getEntityMap();
		for ( EntityKey key : entityMap.keySet() ) {
			for ( EntityKeyMetadata metadata : metadatas ) {
				if ( key.getTable().equals( metadata.getTable() ) ) {
					consumer.consume( new Tuple( new MapTupleSnapshot( entityMap.get( key ) ) ) );
				}
			}
		}
	}

	@Override
	public TupleIterator executeBackendQuery(BackendCustomQuery customQuery, QueryParameters queryParameters, EntityKeyMetadata[] metadatas) {
		throw new UnsupportedOperationException( "Native queries not supported for Map" );
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return new NoOpParameterMetadataBuilder();
	}
}
