/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.ogm.datastore.infinispan.dialect.impl.InfinispanPessimisticWriteLockingStrategy;
import org.hibernate.ogm.datastore.infinispan.dialect.impl.InfinispanTupleSnapshot;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.KeyProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.LocalCacheManager;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.LocalCacheManager.Bucket;
import org.hibernate.ogm.datastore.map.impl.MapAssociationSnapshot;
import org.hibernate.ogm.datastore.map.impl.MapHelpers;
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
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.atomic.FineGrainedAtomicMap;
import org.infinispan.context.Flag;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.distexec.mapreduce.Reducer;

/**
 * EK is the entity cache key type
 * AK is the association cache key type
 * ISK is the identity source cache key type
 *
 * @author Emmanuel Bernard
 */
public class InfinispanDialect<EK,AK,ISK> extends BaseGridDialect {

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
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
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
			return new Tuple( new InfinispanTupleSnapshot( atomicMap ) );
		}
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		//TODO we don't verify that it does not yet exist assuming that this has been done before by the calling code
		//should we improve?
		Cache<EK, Map<String, Object>> cache = getCacheManager().getEntityCache( key.getMetadata() );
		EK cacheKey = getKeyProvider().getEntityCacheKey( key );
		FineGrainedAtomicMap<String,Object> atomicMap =  AtomicMapLookup.getFineGrainedAtomicMap( cache, cacheKey, true );
		return new Tuple( new InfinispanTupleSnapshot( atomicMap ) );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
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
		//TODO we don't verify that it does not yet exist assuming that this ahs been done before by the calling code
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
	public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		Set<Bucket<EK>> buckets = getCacheManager().getWorkBucketsFor(
				entityKeyMetadatas
		);
		for ( Bucket<EK> bucket : buckets ) {
			Map<EK, Map<String, Object>> queryResult = retrieveKeys( bucket.getCache(), bucket.getEntityKeyMetadata() );
			for ( Entry<EK, Map<String, Object>> entry : queryResult.entrySet() ) {
				consumer.consume( getTupleFromCacheKey( entry.getKey(), bucket.getCache() ) );
			}
		}
	}

	private Map<EK, Map<String, Object>> retrieveKeys(Cache<EK, Map<String, Object>> cache, EntityKeyMetadata... entityKeyMetadatas) {
		MapReduceTask<EK, Map<String, Object>, EK, Map<String, Object>> queryTask = new MapReduceTask<EK, Map<String, Object>, EK, Map<String, Object>>( cache );
		queryTask.mappedWith( getKeyProvider().getMapper( entityKeyMetadatas ) ).reducedWith( new TupleReducer<EK>() );
		return queryTask.execute();
	}

	@SuppressWarnings("unchecked")
	private LocalCacheManager<EK, AK, ISK> getCacheManager() {
		return (LocalCacheManager<EK, AK, ISK>) provider.getCacheManager();
	}

	@SuppressWarnings("unchecked")
	private KeyProvider<EK, AK, ISK> getKeyProvider() {
		return (KeyProvider<EK, AK, ISK>) provider.getKeyProvider();
	}

	static class TupleReducer<EK> implements Reducer<EK, Map<String, Object>> {

		@Override
		public Map<String, Object> reduce(EK reducedKey, Iterator<Map<String, Object>> iter) {
			return iter.next();
		}

	}
}
