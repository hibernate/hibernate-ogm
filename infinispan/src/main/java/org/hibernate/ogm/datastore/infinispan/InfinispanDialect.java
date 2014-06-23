/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan;

import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ASSOCIATION_STORE;
import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ENTITY_STORE;
import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.IDENTIFIER_STORE;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.infinispan.dialect.impl.InfinispanPessimisticWriteLockingStrategy;
import org.hibernate.ogm.datastore.infinispan.dialect.impl.InfinispanTupleSnapshot;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.map.impl.MapAssociationSnapshot;
import org.hibernate.ogm.datastore.map.impl.MapHelpers;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.IdGeneratorKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.query.NoOpParameterMetadataBuilder;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.util.ClosableIterator;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.atomic.FineGrainedAtomicMap;
import org.infinispan.context.Flag;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.MapReduceTask;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;

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
		if ( lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_WRITE ) {
			return new InfinispanPessimisticWriteLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_READ ) {
			// TODO find a more efficient pessimistic read
			return new InfinispanPessimisticWriteLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC ) {
			return new OptimisticLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT ) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		throw new UnsupportedOperationException( "LockMode " + lockMode
				+ " is not supported by the Infinispan GridDialect" );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		Cache<EntityKey, Map<String, Object>> cache = provider.getCache( ENTITY_STORE );
		FineGrainedAtomicMap<String, Object> atomicMap = AtomicMapLookup.getFineGrainedAtomicMap( cache, key, false );
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
		Cache<EntityKey, Map<String, Object>> cache = provider.getCache( ENTITY_STORE );
		FineGrainedAtomicMap<String,Object> atomicMap =  AtomicMapLookup.getFineGrainedAtomicMap( cache, key, true );
		return new Tuple( new InfinispanTupleSnapshot( atomicMap ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		Map<String,Object> atomicMap = ( (InfinispanTupleSnapshot) tuple.getSnapshot() ).getAtomicMap();
		MapHelpers.applyTupleOpsOnMap( tuple, atomicMap );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		Cache<EntityKey, Map<String, Object>> cache = provider.getCache( ENTITY_STORE );
		AtomicMapLookup.removeAtomicMap( cache, key );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache = provider.getCache( ASSOCIATION_STORE );
		Map<RowKey, Map<String, Object>> atomicMap = AtomicMapLookup.getFineGrainedAtomicMap( cache, key, false );
		return atomicMap == null ? null : new Association( new MapAssociationSnapshot( atomicMap ) );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		//TODO we don't verify that it does not yet exist assuming that this ahs been done before by the calling code
		//should we improve?
		Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache = provider.getCache( ASSOCIATION_STORE );
		Map<RowKey, Map<String, Object>> atomicMap = AtomicMapLookup.getFineGrainedAtomicMap( cache, key, true );
		return new Association( new MapAssociationSnapshot( atomicMap ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext) {
		MapHelpers.updateAssociation( association, key );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		Cache<AssociationKey, Map<RowKey, Map<String, Object>>> cache = provider.getCache( ASSOCIATION_STORE );
		AtomicMapLookup.removeAtomicMap( cache, key );
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
	public void nextValue(IdGeneratorKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		final AdvancedCache<IdGeneratorKey, Object> identifierCache = provider.getCache( IDENTIFIER_STORE ).getAdvancedCache();
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
				value.initialize( ( (Number) valueFromDb ).longValue() );
			}

			//update value
			final IntegralDataTypeHolder updateValue = value.copy();
			//increment value
			updateValue.add( increment );
			//TODO should we use GridTypes here?
			final Object newValueFromDb = updateValue.makeValue();
			done = identifierCache.replace( key, valueFromDb, newValueFromDb );
		}
		while ( !done );
	}

	@Override
	public boolean supportsSequences() {
		return false;
	}

	@Override
	public GridType overrideType(Type type) {
		return null;
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendCustomQuery customQuery, QueryParameters queryParameters) {
		throw new UnsupportedOperationException( "Native queries not supported for Infinispan" );
	}

	@Override
	@SuppressWarnings("unchecked")
	public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		Cache<EntityKey, Map<String, Object>> cache = provider.getCache( ENTITY_STORE );
		Map<EntityKey, Map<String, Object>> queryResult = retrieveKeys( cache, entityKeyMetadatas );
		for ( Entry<EntityKey, Map<String, Object>> entry : queryResult.entrySet() ) {
			consumer.consume( getTuple( entry.getKey(), null ) );
		}
	}

	private Map<EntityKey, Map<String, Object>> retrieveKeys(Cache<EntityKey, Map<String, Object>> cache, EntityKeyMetadata... entityKeyMetadatas) {
		MapReduceTask<EntityKey, Map<String, Object>, EntityKey, Map<String, Object>> queryTask = new MapReduceTask<EntityKey, Map<String, Object>, EntityKey, Map<String, Object>>( cache );
		queryTask.mappedWith( new TupleMapper( entityKeyMetadatas ) ).reducedWith( new TupleReducer() );
		return queryTask.execute();
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return NoOpParameterMetadataBuilder.INSTANCE;
	}

	static class TupleMapper implements Mapper<EntityKey, Map<String, Object>, EntityKey, Map<String, Object>> {

		private final EntityKeyMetadata[] entityKeyMetadatas;

		public TupleMapper(EntityKeyMetadata... entityKeyMetadatas) {
			this.entityKeyMetadatas = entityKeyMetadatas;
		}

		@Override
		public void map(EntityKey key, Map<String, Object> value, Collector<EntityKey, Map<String, Object>> collector) {
			for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
				if ( key.getTable().equals( entityKeyMetadata.getTable() ) ) {
					collector.emit( key, value );
				}
			}
		}

	}

	static class TupleReducer implements Reducer<EntityKey, Map<String, Object>> {

		@Override
		public Map<String, Object> reduce(EntityKey reducedKey, Iterator<Map<String, Object>> iter) {
			return iter.next();
		}

	}
}
