/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.ehcache;

import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.Element;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableKey;
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableMapAssociationSnapshot;
import org.hibernate.ogm.datastore.ehcache.impl.Cache;
import org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider;
import org.hibernate.ogm.datastore.map.impl.MapHelpers;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.AssociationOperation;
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
		final Cache<SerializableKey> entityCache = datastoreProvider.getEntityCache();
		final Element element = entityCache.get( new SerializableKey( key ) );
		if ( element != null ) {
			return createTuple( element );
		}
		else {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private Tuple createTuple(final Element element) {
		return new Tuple( new MapTupleSnapshot( (Map<String, Object>) element.getObjectValue() ) );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		final Cache<SerializableKey> entityCache = datastoreProvider.getEntityCache();
		final HashMap<String, Object> tuple = new HashMap<String, Object>();
		entityCache.put( new Element( new SerializableKey( key ), tuple ) );

		return new Tuple( new MapTupleSnapshot( tuple ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		Map<String, Object> entityRecord = ( (MapTupleSnapshot) tuple.getSnapshot() ).getMap();
		MapHelpers.applyTupleOpsOnMap( tuple, entityRecord );

		final Cache<SerializableKey> entityCache = datastoreProvider.getEntityCache();
		entityCache.put( new Element( new SerializableKey( key ), entityRecord ) );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		datastoreProvider.getEntityCache().remove( new SerializableKey( key ) );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		final Cache<SerializableKey> associationCache = datastoreProvider.getAssociationCache();
		final Element element = associationCache.get( new SerializableKey( key ) );

		if ( element == null ) {
			return null;
		}
		else {
			@SuppressWarnings("unchecked")
			Map<SerializableKey, Map<String, Object>> associationRows = (Map<SerializableKey, Map<String, Object>>) element.getObjectValue();
			return new Association( new SerializableMapAssociationSnapshot( associationRows ) );
		}
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		final Cache<SerializableKey> associationCache = datastoreProvider.getAssociationCache();
		Map<SerializableKey, Map<String, Object>> association = new HashMap<SerializableKey, Map<String, Object>>();
		associationCache.put( new Element( new SerializableKey( key ), association ) );
		return new Association( new SerializableMapAssociationSnapshot( association ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext) {
		Map<SerializableKey, Map<String, Object>> associationRows = ( (SerializableMapAssociationSnapshot) association.getSnapshot() ).getUnderlyingMap();

		for ( AssociationOperation action : association.getOperations() ) {
			switch ( action.getType() ) {
				case CLEAR:
					associationRows.clear();
				case PUT_NULL:
				case PUT:
					associationRows.put( new SerializableKey( action.getKey() ), MapHelpers.tupleToMap( action.getValue() ) );
					break;
				case REMOVE:
					associationRows.remove( new SerializableKey( action.getKey() ) );
					break;
			}
		}

		final Cache<SerializableKey> associationCache = datastoreProvider.getAssociationCache();
		associationCache.put( new Element( new SerializableKey( key ), associationRows ) );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		datastoreProvider.getAssociationCache().remove( new SerializableKey( key ) );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return new Tuple();
	}

	@Override
	public void nextValue(RowKey rowKey, IntegralDataTypeHolder value, int increment, int initialValue) {
		final Cache<SerializableKey> cache = datastoreProvider.getIdentifierCache();
		SerializableKey key = new SerializableKey( rowKey );

		Element previousValue = cache.get( key );
		if ( previousValue == null ) {
			previousValue = cache.putIfAbsent( new Element( key, initialValue ) );
		}
		if ( previousValue != null ) {
			while ( !cache.replace( previousValue,
					new Element( key, ( (Integer) previousValue.getObjectValue() ) + increment ) ) ) {
				previousValue = cache.get( key );
			}
			value.initialize( ( (Integer) previousValue.getObjectValue() ) + increment );
		}
		else {
			value.initialize( initialValue );
		}
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
		return false;
	}

	@Override
	public GridType overrideType(Type type) {
		return null;
	}

	@Override
	public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		Cache<SerializableKey> entityCache = datastoreProvider.getEntityCache();
		for ( SerializableKey key : entityCache.getKeys() ) {
			for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
				// Check if there is a way to load keys applying a filter
				if ( key.getTable().equals( entityKeyMetadata.getTable() ) ) {
					Element element = entityCache.get( key );
					consumer.consume( createTuple( element ) );
				}
			}
		}
	}

	@Override
	public TupleIterator executeBackendQuery(BackendCustomQuery customQuery, QueryParameters queryParameters, EntityKeyMetadata[] metadatas) {
		throw new UnsupportedOperationException( "Native queries not supported for Ehcache" );
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return new NoOpParameterMetadataBuilder();
	}
}
