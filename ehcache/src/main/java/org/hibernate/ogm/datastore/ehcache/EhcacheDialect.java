/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableAssociationKey;
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableEntityKey;
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableIdSourceKey;
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableMapAssociationSnapshot;
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableRowKey;
import org.hibernate.ogm.datastore.ehcache.impl.Cache;
import org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider;
import org.hibernate.ogm.datastore.map.impl.MapHelpers;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
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
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationOperation;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.persister.entity.Lockable;

/**
 * @author Alex Snaps
 */
public class EhcacheDialect extends BaseGridDialect {

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
		final Cache<SerializableEntityKey> entityCache = datastoreProvider.getEntityCache();
		final Element element = entityCache.get( new SerializableEntityKey( key ) );
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
		final Cache<SerializableEntityKey> entityCache = datastoreProvider.getEntityCache();
		final HashMap<String, Object> tuple = new HashMap<String, Object>();
		entityCache.put( new Element( new SerializableEntityKey( key ), tuple ) );

		return new Tuple( new MapTupleSnapshot( tuple ) );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		Map<String, Object> entityRecord = ( (MapTupleSnapshot) tuple.getSnapshot() ).getMap();
		MapHelpers.applyTupleOpsOnMap( tuple, entityRecord );

		final Cache<SerializableEntityKey> entityCache = datastoreProvider.getEntityCache();
		entityCache.put( new Element( new SerializableEntityKey( key ), entityRecord ) );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		datastoreProvider.getEntityCache().remove( new SerializableEntityKey( key ) );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		final Cache<SerializableAssociationKey> associationCache = datastoreProvider.getAssociationCache();
		final Element element = associationCache.get( new SerializableAssociationKey( key ) );

		if ( element == null ) {
			return null;
		}
		else {
			@SuppressWarnings("unchecked")
			Map<SerializableRowKey, Map<String, Object>> associationRows = (Map<SerializableRowKey, Map<String, Object>>) element.getObjectValue();
			return new Association( new SerializableMapAssociationSnapshot( associationRows ) );
		}
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		final Cache<SerializableAssociationKey> associationCache = datastoreProvider.getAssociationCache();
		Map<SerializableRowKey, Map<String, Object>> association = new HashMap<SerializableRowKey, Map<String, Object>>();
		associationCache.put( new Element( new SerializableAssociationKey( key ), association ) );
		return new Association( new SerializableMapAssociationSnapshot( association ) );
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		Map<SerializableRowKey, Map<String, Object>> associationRows = ( (SerializableMapAssociationSnapshot) association.getSnapshot() ).getUnderlyingMap();

		for ( AssociationOperation action : association.getOperations() ) {
			switch ( action.getType() ) {
				case CLEAR:
					associationRows.clear();
				case PUT_NULL:
				case PUT:
					associationRows.put( new SerializableRowKey( action.getKey() ), MapHelpers.tupleToMap( action.getValue() ) );
					break;
				case REMOVE:
					associationRows.remove( new SerializableRowKey( action.getKey() ) );
					break;
			}
		}

		final Cache<SerializableAssociationKey> associationCache = datastoreProvider.getAssociationCache();
		associationCache.put( new Element( new SerializableAssociationKey( key ), associationRows ) );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		datastoreProvider.getAssociationCache().remove( new SerializableAssociationKey( key ) );
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		final Cache<SerializableIdSourceKey> cache = datastoreProvider.getIdentifierCache();
		SerializableIdSourceKey key = new SerializableIdSourceKey( request.getKey() );

		Element previousValue = cache.get( key );
		if ( previousValue == null ) {
			previousValue = cache.putIfAbsent( new Element( key, request.getInitialValue() ) );
		}
		if ( previousValue != null ) {
			while ( !cache.replace( previousValue,
					new Element( key, ( (Integer) previousValue.getObjectValue() ) + request.getIncrement() ) ) ) {
				previousValue = cache.get( key );
			}
			return ( (Integer) previousValue.getObjectValue() ) + request.getIncrement();
		}
		else {
			return request.getInitialValue();
		}
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		Cache<SerializableEntityKey> entityCache = datastoreProvider.getEntityCache();
		for ( SerializableEntityKey key : entityCache.getKeys() ) {
			for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
				// Check if there is a way to load keys applying a filter
				if ( key.getTable().equals( entityKeyMetadata.getTable() ) ) {
					Element element = entityCache.get( key );
					consumer.consume( createTuple( element ) );
				}
			}
		}
	}
}
