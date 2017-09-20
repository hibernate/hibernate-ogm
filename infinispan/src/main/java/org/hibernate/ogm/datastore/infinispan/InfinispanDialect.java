/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.ogm.datastore.infinispan.dialect.impl.InfinispanPessimisticWriteLockingStrategy;
import org.hibernate.ogm.datastore.infinispan.dialect.impl.InfinispanTupleSnapshot;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanEmbeddedDatastoreProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.KeyProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.LocalCacheManager;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.LocalCacheManager.Bucket;
import org.hibernate.ogm.datastore.map.impl.MapAssociationSnapshot;
import org.hibernate.ogm.datastore.map.impl.MapHelpers;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.dialect.spi.TuplesSupplier;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.persister.entity.Lockable;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.atomic.FineGrainedAtomicMap;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.Flag;
import org.infinispan.stream.CacheCollectors;

/**
 * EK is the entity cache key type
 * AK is the association cache key type
 * ISK is the identity source cache key type
 *
 * @author Emmanuel Bernard
 */
public class InfinispanDialect<EK,AK,ISK> extends BaseGridDialect {

	private final InfinispanEmbeddedDatastoreProvider provider;

	public InfinispanDialect(InfinispanEmbeddedDatastoreProvider provider) {
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
		if ( lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_WRITE ) {
			return new InfinispanPessimisticWriteLockingStrategy<EK>( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_READ ) {
			// TODO find a more efficient pessimistic read
			return new InfinispanPessimisticWriteLockingStrategy<EK>( lockable, lockMode );
		}
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
	public Tuple getTuple(EntityKey key, OperationContext operationContext) {
		EK cacheKey = getKeyProvider().getEntityCacheKey( key );
		Cache<EK, Map<String, Object>> cache = getCacheManager().getEntityCache( key.getMetadata() );
		return getTupleFromCacheKey( cacheKey, cache );
	}

	private Tuple getTupleFromCacheKey(EK cacheKey, Cache<EK, Map<String,Object>> cache) {
		FineGrainedAtomicMap<String, Object> atomicMap = AtomicMapLookup.getFineGrainedAtomicMap(
				cache,
				cacheKey,
				false
		);
		if ( atomicMap == null ) {
			return null;
		}
		else {
			return new Tuple( new InfinispanTupleSnapshot( atomicMap ), SnapshotType.UPDATE );
		}
	}

	@Override
	public Tuple createTuple(EntityKey key, OperationContext operationContext) {
		//TODO we don't verify that it does not yet exist assuming that this has been done before by the calling code
		//should we improve?
		Cache<EK, Map<String, Object>> cache = getCacheManager().getEntityCache( key.getMetadata() );
		EK cacheKey = getKeyProvider().getEntityCacheKey( key );
		FineGrainedAtomicMap<String,Object> atomicMap =  AtomicMapLookup.getFineGrainedAtomicMap( cache, cacheKey, true );
		return new Tuple( new InfinispanTupleSnapshot( atomicMap ), SnapshotType.INSERT );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) {
		Tuple tuple = tuplePointer.getTuple();
		Map<String,Object> atomicMap = ( (InfinispanTupleSnapshot) tuple.getSnapshot() ).getAtomicMap();
		MapHelpers.applyTupleOpsOnMap( tuple, atomicMap );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		Cache<EK, Map<String, Object>> cache = getCacheManager().getEntityCache( key.getMetadata() );
		EK cacheKey = getKeyProvider().getEntityCacheKey( key );
		AtomicMapLookup.removeAtomicMap( cache, cacheKey );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		Cache<AK, Map<RowKey, Map<String, Object>>> cache = getCacheManager().getAssociationCache(
				key.getMetadata()
		);
		AK cacheKey = getKeyProvider().getAssociationCacheKey( key );
		Map<RowKey, Map<String, Object>> atomicMap = AtomicMapLookup.getFineGrainedAtomicMap( cache, cacheKey, false );
		return atomicMap == null ? null : new Association( new MapAssociationSnapshot( atomicMap ) );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		//TODO we don't verify that it does not yet exist assuming that this has been done before by the calling code
		//should we improve?
		Cache<AK, Map<RowKey, Map<String, Object>>> cache = getCacheManager().getAssociationCache(
				key.getMetadata()
		);
		AK cacheKey = getKeyProvider().getAssociationCacheKey( key );
		Map<RowKey, Map<String, Object>> atomicMap = AtomicMapLookup.getFineGrainedAtomicMap( cache, cacheKey, true );
		return new Association( new MapAssociationSnapshot( atomicMap ) );
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		MapHelpers.updateAssociation( association );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		Cache<AK, Map<RowKey, Map<String, Object>>> cache = getCacheManager().getAssociationCache(
				key.getMetadata()
		);
		AK cacheKey = getKeyProvider().getAssociationCacheKey( key );
		AtomicMapLookup.removeAtomicMap( cache, cacheKey );
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	//TODO should we use GridTypes here?
	public Number nextValue(NextValueRequest request) {
		final AdvancedCache<ISK, Object> identifierCache = getCacheManager()
				.getIdSourceCache( request.getKey().getMetadata() )
				.getAdvancedCache();
		ISK cacheKey = getKeyProvider().getIdSourceCacheKey( request.getKey() );
		boolean done;
		Number value;

		do {
			//skip locking proposed by Sanne
			value = (Number) identifierCache.withFlags( Flag.SKIP_LOCKING ).get( cacheKey );

			if ( value == null ) {
				value = Long.valueOf( request.getInitialValue() );
				final Number oldValue = (Number) identifierCache.putIfAbsent( cacheKey, value );
				if ( oldValue != null ) {
					value = oldValue;
				}
			}

			Number newValue = value.longValue() + request.getIncrement();
			done = identifierCache.replace( cacheKey, value, newValue );
		}
		while ( !done );

		return value;
	}

	@Override
	public void forEachTuple( ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata ) {
		Set<Bucket<EK>> buckets = getCacheManager().getWorkBucketsFor( entityKeyMetadata );

		for ( Bucket<EK> bucket : buckets ) {
			Map<EK, Map<String, Object>> queryResult = new HashMap<>();

			List<CacheEntry<EK, Map<String, Object>>> collect = bucket.getCache().getAdvancedCache().cacheEntrySet()
				.stream()
				.filter( getKeyProvider().getFilter( entityKeyMetadata ) )
				// also collector needs to be Serializable (for non local caches)
				.collect( CacheCollectors.serializableCollector( () -> Collectors.toList() ) );

			for ( CacheEntry<EK, Map<String, Object>> entry : collect ) {
				queryResult.put( entry.getKey(), entry.getValue() );
			}

			// At runtime values of queryResult will be members of class org.infinispan.atomic.impl.AtomicKeySetImpl
			// this is because of the new implementation of FineGrainedAtomicMap Infinispan class (since 9.1)
			// query result return anyway valid keys, the values will be reloaded later by the InfinispanTupleIterator
			InfinispanTuplesSupplier<EK> supplier = new InfinispanTuplesSupplier( bucket.getCache(), queryResult );
			consumer.consume( supplier );
		}
	}

	@SuppressWarnings("unchecked")
	private LocalCacheManager<EK, AK, ISK> getCacheManager() {
		return (LocalCacheManager<EK, AK, ISK>) provider.getCacheManager();
	}

	@SuppressWarnings("unchecked")
	private KeyProvider<EK, AK, ISK> getKeyProvider() {
		return (KeyProvider<EK, AK, ISK>) provider.getKeyProvider();
	}

	private class InfinispanTuplesSupplier<SEK> implements TuplesSupplier {

		private final Map<SEK, Map<String, Object>> queryResult;
		private final Cache<SEK, Map<String, Object>> cache;

		public InfinispanTuplesSupplier(Cache<SEK, Map<String, Object>> cache, Map<SEK, Map<String, Object>> queryResult) {
			this.cache = cache;
			this.queryResult = queryResult;
		}

		@Override
		public ClosableIterator<Tuple> get(TransactionContext transactionContext) {
			Iterator<Entry<SEK, Map<String, Object>>> iterator = queryResult.entrySet().iterator();
			return new InfinispanTupleIterator( cache, iterator );
		}
	}

	private class InfinispanTupleIterator<IEK> implements ClosableIterator<Tuple> {

		private final Iterator<Entry<IEK, Map<String, Object>>> iterator;
		private final Cache<IEK, Map<String, Object>> cache;

		public InfinispanTupleIterator(Cache<IEK, Map<String, Object>> cache, Iterator<Entry<IEK, Map<String, Object>>> iterator) {
			this.cache = cache;
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public Tuple next() {
			Entry<IEK, Map<String, Object>> entry = iterator.next();
			return getTupleFromCacheKey( (EK) entry.getKey(), (Cache<EK, Map<String, Object>>) cache );
		}

		@Override
		public void close() {
		}
	}
}
