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
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableMapAssociationSnapshot;
import org.hibernate.ogm.datastore.ehcache.impl.Cache;
import org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.common.impl.SerializableRowKey;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl.KeyProvider;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl.LocalCacheManager;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl.LocalCacheManager.KeyProcessor;
import org.hibernate.ogm.datastore.map.impl.MapHelpers;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
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
 * Persists domain models in the Ehcache key/value store.
 *
 * @author Alex Snaps
 * @author Gunnar Morling
 *
 * @param <EK> the entity cache key type
 * @param <AK> the association cache key type
 * @param <ISK> the identity source cache key type
 */
public class EhcacheDialect<EK, AK, ISK> extends BaseGridDialect {

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
		else {
			return null;
		}
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		final Cache<EK> entityCache = getCacheManager().getEntityCache( key.getMetadata() );
		final Element element = entityCache.get( getKeyProvider().getEntityCacheKey( key ) );
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
		return new Tuple( new MapTupleSnapshot( new HashMap<String, Object>() ) );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		Cache<EK> entityCache = getCacheManager().getEntityCache( key.getMetadata() );

		Map<String, Object> entityRecord = ( (MapTupleSnapshot) tuple.getSnapshot() ).getMap();

		if ( entityRecord.isEmpty() ) {
			MapHelpers.applyTupleOpsOnMap( tuple, entityRecord );
			Element previous = entityCache.putIfAbsent( new Element( getKeyProvider().getEntityCacheKey( key ), entityRecord ) );
			if ( previous != null ) {
				throw new TupleAlreadyExistsException( key.getMetadata(), tuple, null );
			}
		}
		else {
			MapHelpers.applyTupleOpsOnMap( tuple, entityRecord );
			entityCache.put( new Element( getKeyProvider().getEntityCacheKey( key ), entityRecord ) );
		}
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		getCacheManager().getEntityCache( key.getMetadata() ).remove( getKeyProvider().getEntityCacheKey( key ) );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		final Cache<AK> associationCache = getCacheManager().getAssociationCache( key.getMetadata() );
		final Element element = associationCache.get( getKeyProvider().getAssociationCacheKey( key ) );

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
		final Cache<AK> associationCache = getCacheManager().getAssociationCache( key.getMetadata() );
		Map<SerializableRowKey, Map<String, Object>> association = new HashMap<SerializableRowKey, Map<String, Object>>();
		associationCache.put( new Element( getKeyProvider().getAssociationCacheKey( key ), association ) );
		return new Association( new SerializableMapAssociationSnapshot( association ) );
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		Map<SerializableRowKey, Map<String, Object>> associationRows = ( (SerializableMapAssociationSnapshot) association.getSnapshot() ).getUnderlyingMap();

		for ( AssociationOperation action : association.getOperations() ) {
			switch ( action.getType() ) {
				case CLEAR:
					associationRows.clear();
				case PUT:
					associationRows.put( new SerializableRowKey( action.getKey() ), MapHelpers.associationRowToMap( action.getValue() ) );
					break;
				case REMOVE:
					associationRows.remove( new SerializableRowKey( action.getKey() ) );
					break;
			}
		}

		final Cache<AK> associationCache = getCacheManager().getAssociationCache( key.getMetadata() );
		associationCache.put( new Element( getKeyProvider().getAssociationCacheKey( key ), associationRows ) );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		getCacheManager().getAssociationCache( key.getMetadata() ).remove( getKeyProvider().getAssociationCacheKey( key ) );
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		final Cache<ISK> cache = getCacheManager().getIdSourceCache( request.getKey().getMetadata() );
		ISK key = getKeyProvider().getIdSourceCacheKey( request.getKey() );

		Element previousValue = cache.get( key );
		if ( previousValue == null ) {
			previousValue = cache.putIfAbsent( new Element( key, request.getInitialValue() ) );
		}
		if ( previousValue != null ) {
			while ( !cache.replace( previousValue,
					new Element( key, ( (Number) previousValue.getObjectValue() ).longValue() + request.getIncrement() ) ) ) {
				previousValue = cache.get( key );
			}
			return ( (Number) previousValue.getObjectValue() ).longValue() + request.getIncrement();
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
		getCacheManager().forEachTuple( new EntityKeyProcessor( consumer ), entityKeyMetadatas );
	}

	@Override
	public DuplicateInsertPreventionStrategy getDuplicateInsertPreventionStrategy(EntityKeyMetadata entityKeyMetadata) {
		return DuplicateInsertPreventionStrategy.NATIVE;
	}

	@SuppressWarnings("unchecked")
	private LocalCacheManager<EK, AK, ISK> getCacheManager() {
		return (LocalCacheManager<EK, AK, ISK>) datastoreProvider.getCacheManager();
	}

	@SuppressWarnings("unchecked")
	private KeyProvider<EK, AK, ISK> getKeyProvider() {
		return (KeyProvider<EK, AK, ISK>) datastoreProvider.getKeyProvider();
	}

	private class EntityKeyProcessor implements KeyProcessor<EK> {

		private final ModelConsumer consumer;

		private EntityKeyProcessor(ModelConsumer consumer) {
			this.consumer = consumer;
		}

		@Override
		public void processKey(EK key, Cache<EK> cache) {
			Element element = cache.get( key );
			consumer.consume( createTuple( element ) );
		}
	}
}
