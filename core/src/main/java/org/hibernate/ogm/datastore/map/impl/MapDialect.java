/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.map.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.persister.entity.Lockable;

/**
 * Grid dialect which uses a plain map for storing objects in memory. For testing purposes.
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
public class MapDialect extends BaseGridDialect {

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
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
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
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		MapHelpers.updateAssociation( association, key );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		provider.removeAssociation( key );
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return provider.getSharedAtomicInteger( request.getKey(), request.getInitialValue(), request.getIncrement() );
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... metadatas) {
		Map<EntityKey, Map<String, Object>> entityMap = provider.getEntityMap();
		for ( EntityKey key : entityMap.keySet() ) {
			for ( EntityKeyMetadata metadata : metadatas ) {
				if ( key.getTable().equals( metadata.getTable() ) ) {
					consumer.consume( new Tuple( new MapTupleSnapshot( entityMap.get( key ) ) ) );
				}
			}
		}
	}
}
