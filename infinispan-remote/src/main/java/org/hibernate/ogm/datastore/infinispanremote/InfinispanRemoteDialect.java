/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.impl.ProtoStreamMappingAdapter;
import org.hibernate.ogm.datastore.infinispanremote.impl.ProtostreamAssociationMappingAdapter;
import org.hibernate.ogm.datastore.infinispanremote.impl.VersionedTuple;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamId;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamPayload;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.map.impl.MapAssociationSnapshot;
import org.hibernate.ogm.datastore.map.impl.MapHelpers;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.dialect.batch.spi.GroupedChangesToEntityOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateTupleOperation;
import org.hibernate.ogm.dialect.batch.spi.Operation;
import org.hibernate.ogm.dialect.batch.spi.RemoveAssociationOperation;
import org.hibernate.ogm.dialect.impl.AbstractGroupingByEntityDialect;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.dialect.spi.TuplesSupplier;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationOperation;
import org.hibernate.ogm.model.spi.AssociationOperationType;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.query.dsl.FilterConditionContext;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.QueryFactory;

/**
 *  Some implementation notes for evolution:
 *
 *  - QueryableGridDialect can't be implemented as "native queries" in Hot Rod are DSL based
 *    and have no String representation; this might change as Hot Rod exposes the underlying
 *    query representation which is similar to HQL; alternatively we could look at exposing
 *    native queries in some way other than a String.
 *
 *  - OptimisticLockingAwareGridDialect can't be implemented as it requires an atomic replace
 *    operation on a subset of the columns, while atomic operations in Hot Rod have to involve
 *    either the full value or the version metadata of Infinispan's VersionedValue.
 *
 *  - IdentityColumnAwareGridDialect can't work out of the box. I suspect we could do this but
 *    would need extending the Infinispan server deployment with some extension such as a
 *    custom script to be invoked from the client.
 *
 *  - BatchableGridDialect could probably be implemented.
 *
 * @author Sanne Grinovero
 */
public class InfinispanRemoteDialect<EK,AK,ISK> extends AbstractGroupingByEntityDialect implements MultigetGridDialect {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final InfinispanRemoteDatastoreProvider provider;

	public InfinispanRemoteDialect(InfinispanRemoteDatastoreProvider provider) {
		this.provider = Objects.requireNonNull( provider );
	}

	@Override
	public Tuple getTuple(EntityKey key, OperationContext operationContext) {
		return getTuple( provider, key );
	}

	private static Tuple getTuple(InfinispanRemoteDatastoreProvider provider, EntityKey key) {
		final String cacheName = cacheName( key );
		ProtoStreamMappingAdapter mapper = provider.getDataMapperForCache( cacheName );
		ProtostreamId idBuffer = mapper.createIdPayload( key.getColumnNames(), key.getColumnValues() );
		VersionedValue<ProtostreamPayload> v = mapper.withinCacheEncodingContext( c -> c.getVersioned( idBuffer ) );
		if ( v == null ) {
			return null;
		}
		ProtostreamPayload payload = v.getValue();
		if ( payload == null ) {
			return null;
		}
		long version = v.getVersion();
		VersionedTuple versionedTuple = payload.toVersionedTuple( SnapshotType.UPDATE );
		versionedTuple.setVersion( version );
		return versionedTuple;
	}

	@Override
	public Tuple createTuple(EntityKey key, OperationContext operationContext) {
		return new VersionedTuple();
	}

	@Override
	protected void executeGroupedChangesToEntity(GroupedChangesToEntityOperation groupedOperation) {
		final EntityKey entityKey = groupedOperation.getEntityKey();
		final String cacheName = cacheName( entityKey );
		final OwningEntity owningEntity = new OwningEntity( provider, entityKey );

		for ( Operation operation : groupedOperation.getOperations() ) {
			if ( operation instanceof InsertOrUpdateTupleOperation ) {
				InsertOrUpdateTupleOperation insertOrUpdateTupleOperation = (InsertOrUpdateTupleOperation) operation;
				Tuple tuple = insertOrUpdateTupleOperation.getTuplePointer().getTuple();
				owningEntity.applyOperations( tuple );
			}
			else if ( operation instanceof InsertOrUpdateAssociationOperation ) {
				insertOrUpdateAssociation( (InsertOrUpdateAssociationOperation) operation );
			}
			else if ( operation instanceof RemoveAssociationOperation ) {
				log.debugf( "removeAssociation for key '%s' on cache '%s'", entityKey, cacheName );
				RemoveAssociationOperation removeAssociationOperation = (RemoveAssociationOperation) operation;
				owningEntity.removeAssociation( removeAssociationOperation );
			}
			else {
				throw new IllegalStateException( operation.getClass().getSimpleName() + " not supported here" );
			}
		}

		owningEntity.flushOperations();
	}

	/**
	 * Applies the grouped operations to the selected entity.
	 */
	private static class OwningEntity {

		private final InfinispanRemoteDatastoreProvider provider;

		// Keep track of the association to remove that are not contained in the entity
		private final List<AssociationKey> associationsToRemove = new ArrayList<>();

		private final EntityKey ownerEntityKey;

		// A representation of the entity that we want to create or insert
		private Map<String, Object> owningEntity;

		// If the entity already exists in the datastore or not
		private SnapshotType operationType = SnapshotType.UPDATE;

		public OwningEntity(InfinispanRemoteDatastoreProvider provider, EntityKey entityKey) {
			this.provider = provider;
			this.ownerEntityKey = entityKey;
		}

		public void flushOperations() {
			if ( !associationsToRemove.isEmpty() ) {
				for ( AssociationKey key : associationsToRemove ) {
					removeAssociationFromBridgeTable( provider, key );
				}
			}

			if ( owningEntity != null ) {
				flushEntity();
			}
		}

		private void flushEntity() {
			Tuple versionedTuple = new Tuple( new MapTupleSnapshot( owningEntity ), operationType );
			ProtoStreamMappingAdapter mapper = provider.getDataMapperForCache( cacheName( ownerEntityKey ) );
			ProtostreamId idBuffer = mapper.createIdPayload( ownerEntityKey.getColumnNames(), ownerEntityKey.getColumnValues() );
			ProtostreamPayload valuePayload = mapper.createValuePayload( versionedTuple );

			if ( operationType == SnapshotType.INSERT ) {
				insertEntity( mapper, idBuffer, valuePayload );
			}
			else {
				updateEntity( mapper, idBuffer, valuePayload );
			}
		}

		private void updateEntity(ProtoStreamMappingAdapter mapper, ProtostreamId idBuffer, ProtostreamPayload valuePayload) {
			mapper.withinCacheEncodingContext( c -> c.put( idBuffer, valuePayload ) );
		}

		private void insertEntity(ProtoStreamMappingAdapter mapper, ProtostreamId idBuffer, ProtostreamPayload valuePayload) {
			boolean optimisticLockError;
			ProtostreamPayload result = mapper.withinCacheEncodingContext( c -> c.putIfAbsent( idBuffer, valuePayload ) );
			optimisticLockError = null != result;
			if ( optimisticLockError ) {
				throw new TupleAlreadyExistsException( ownerEntityKey );
			}
		}

		public void removeAssociation(RemoveAssociationOperation removeAssociationOperation) {
			AssociationKey associationKey = removeAssociationOperation.getAssociationKey();
			AssociationContext associationContext = removeAssociationOperation.getContext();
			// N.B. 'key' might match multiple entries
			if ( associationStoredWithinEntityEntry( associationKey, associationContext ) ) {
				// The entity contains the association
				if ( owningEntity == null ) {
					TuplePointer entityTuplePointer = getEmbeddingEntityTuplePointer( provider, associationKey, associationContext );
					// We are removing an association inside an entity so this should always be an update
					entityTuplePointer.getTuple().setSnapshotType( SnapshotType.UPDATE );
					applyOperations( entityTuplePointer.getTuple() );
				}
			}
			else {
				// The association is mapped with a bridge "table"
				associationsToRemove.add( associationKey );
			}
		}

		/**
		 * Applies the operations in the tuple to the entity.
		 * <p>
		 * It does not touch the datastore.
		 */
		public void applyOperations(Tuple tuple) {
			if ( owningEntity == null ) {
				owningEntity = getEntityFromTuple( owningEntity, tuple );
			}
			if ( tuple.getSnapshotType() == SnapshotType.INSERT ) {
				operationType = SnapshotType.INSERT;
			}
			MapHelpers.applyTupleOpsOnMap( tuple, owningEntity );
		}

		private Map<String, Object> getEntityFromTuple(Map<String, Object> owningEntity, Tuple tuple) {
			if ( tuple != null ) {
				if ( owningEntity == null ) {
					owningEntity = new HashMap<>();
				}
				for ( String column : tuple.getColumnNames() ) {
					owningEntity.put( column, tuple.get( column ) );
				}
			}
			return owningEntity;
		}
	}

	private void insertOrUpdateAssociation(InsertOrUpdateAssociationOperation insertOrUpdateAssociationOperation) {
		AssociationKey associationKey = insertOrUpdateAssociationOperation.getAssociationKey();
		org.hibernate.ogm.model.spi.Association association = insertOrUpdateAssociationOperation.getAssociation();
		AssociationContext associationContext = insertOrUpdateAssociationOperation.getContext();

		if ( !associationStoredWithinEntityEntry( associationKey, associationContext ) ) {
			insertOrUpdateAssociationMappedAsDedicatedEntries( associationKey, association, associationContext );
		}

		association.reset();
	}

	private static TuplePointer getEmbeddingEntityTuplePointer(InfinispanRemoteDatastoreProvider provider, AssociationKey key, AssociationContext associationContext) {
		TuplePointer tuplePointer = associationContext.getEntityTuplePointer();

		if ( tuplePointer.getTuple() == null ) {
			tuplePointer.setTuple( getTuple( provider, key.getEntityKey() ) );
		}

		return tuplePointer;
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		final String cacheName = cacheName( key );
		log.debugf( "removeTuple for key '%s' on cache '%s'", key, cacheName );
		ProtoStreamMappingAdapter mapper = provider.getDataMapperForCache( cacheName );
		ProtostreamId idBuffer = mapper.createIdPayload( key.getColumnNames(), key.getColumnValues() );
		mapper.withinCacheEncodingContext( c -> c.remove( idBuffer ) );
	}

	private static String cacheName(EntityKey key) {
		return key.getTable();
	}

	private static String cacheName(AssociationKey key) {
		return key.getTable();
	}

	private static String cacheName(EntityKeyMetadata keyMetadata) {
		return keyMetadata.getTable();
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		Map<RowKey, Map<String, Object>> results = loadRowKeysByQuery( provider, key );
		if ( results.isEmpty() ) {
			// For consistency with other dialects,
			// it make it easier to test which operations the dialects executes
			return null;
		}
		return new Association( new MapAssociationSnapshot( results ) );
	}

	private static Map<RowKey, Map<String, Object>> loadRowKeysByQuery(InfinispanRemoteDatastoreProvider provider, AssociationKey key) {
		final String cacheName = cacheName( key );
		ProtostreamAssociationMappingAdapter mapper = provider.getCollectionsDataMapper( cacheName );
		return mapper.withinCacheEncodingContext( c -> {
			QueryFactory queryFactory = Search.getQueryFactory( c );
			final String[] columnNames = key.getColumnNames();
			QueryBuilder qb = queryFactory.from( ProtostreamPayload.class );
			FilterConditionContext bqEnd = null;
			boolean firstIteration = true;
			for ( int i = 0; i < columnNames.length; i++ ) {
				String fieldName = mapper.convertColumnNameToFieldName( columnNames[i] );
				if ( firstIteration ) {
					bqEnd = qb.having( fieldName ).eq( key.getColumnValues()[i] );
					firstIteration = false;
				}
				else {
					bqEnd = bqEnd.and().having( fieldName ).eq( key.getColumnValues()[i] );
				}
			}
			Query query = bqEnd.toBuilder().build();
			Map<RowKey,Map<String, Object>> resultsCollector = new HashMap<>();
			try ( CloseableIterator<Entry<Object,Object>> iterator = c.retrieveEntriesByQuery( query, null, 100 ) ) {
				while ( iterator.hasNext() ) {
					Entry<Object,Object> e  = iterator.next();
					ProtostreamPayload value = ( (ProtostreamPayload) e.getValue() );
					Map<String, Object> entryObject = value.toMap();
					RowKey entryKey = value.asRowKey( key );
					resultsCollector.put( entryKey, entryObject );
				}
			}
			return resultsCollector;
		} );
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		Map<RowKey, Map<String, Object>> associationMap = new HashMap<RowKey, Map<String,Object>>();
		return new Association( new MapAssociationSnapshot( associationMap ) );
	}

	private void insertOrUpdateAssociationMappedAsDedicatedEntries(AssociationKey key, Association association, AssociationContext associationContext) {
		final String cacheName = cacheName( key );
		final ProtoStreamMappingAdapter mapper = provider.getDataMapperForCache( cacheName );
		log.debugf( "insertOrUpdateAssociation for key '%s' on cache '%s', mapped as dedicated entries in ad-hoc table", key, cacheName );
		final List<AssociationOperation> operations = association.getOperations();
		for ( AssociationOperation ao : operations ) {
			AssociationOperationType type = ao.getType();
			RowKey rowKey = ao.getKey();
			ProtostreamId idBuffer = mapper.createIdPayload( rowKey.getColumnNames(), rowKey.getColumnValues() );
			switch ( type ) {
				case PUT:
					ProtostreamPayload valuePayloadForPut = mapper.createValuePayload( ao.getValue() );
					mapper.withinCacheEncodingContext( c -> c.put( idBuffer, valuePayloadForPut ) );
					break;
				case REMOVE:
					mapper.withinCacheEncodingContext( c -> c.remove( idBuffer ) );
					break;
				case CLEAR:
					throw new AssertionFailure( "Request for CLEAR operation on an association mapped to dedicated entries. Makes no sense?" );
			}
		}
	}

	private static void removeAssociationFromBridgeTable(InfinispanRemoteDatastoreProvider provider, AssociationKey key) {
		final String bridgeTable = cacheName( key );
		final ProtoStreamMappingAdapter mapper = provider.getDataMapperForCache( bridgeTable );
		Map<RowKey, Map<String, Object>> rowsMap = loadRowKeysByQuery( provider, key );
		for ( RowKey rowKey : rowsMap.keySet() ) {
			String[] columnNames = rowKey.getColumnNames();
			Object[] columnValues = rowKey.getColumnValues();
			ProtostreamId idBuffer = mapper.createIdPayload( columnNames, columnValues );
			mapper.withinCacheEncodingContext( c -> c.remove( idBuffer ) );
		}
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return provider.getSequenceHandler().getSequenceValue( request );
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata) {
		final String cacheName = cacheName( entityKeyMetadata );
		ProtoStreamMappingAdapter mapper = provider.getDataMapperForCache( cacheName );

		VersionedValue<ProtostreamPayload> v = mapper.withinCacheEncodingContext( c -> {
			consumer.consume( new InfinispanRemoteTuplesSupplier( c, cacheName ) );
			return null;
		} );
	}

	@Override
	public DuplicateInsertPreventionStrategy getDuplicateInsertPreventionStrategy(EntityKeyMetadata entityKeyMetadata) {
		//We can implement duplicate insert detection by this by using Infinispan's putIfAbsent
		//support and verifying the return on any insert
		//TODO Not implemented yet as Hot Rod's support for atomic operations is complex, so default to the naive impl;
		// In particular:
		// - CAS operations such as putIfAbsent require Caches to use Transactions on the server (but the Hot Rod client can't participate)
		// - Multi-Get and Multi-Put operations don't honour the Version API
		return DuplicateInsertPreventionStrategy.LOOK_UP;
	}

	@Override
	public boolean supportsSequences() {
		//For reasons to keep this to 'false' see implementation comments on HotRodSequenceHandler
		return false;
	}

	// [Optional] implement MultigetGridDialect:
	@Override
	public List<Tuple> getTuples(EntityKey[] keys, TupleContext tupleContext) {
		Objects.requireNonNull( keys );
		if ( keys.length == 0 ) {
			return Collections.emptyList();
		}
		else if ( keys.length == 1 ) {
			return Collections.singletonList( getTuple( keys[0], tupleContext ) );
		}
		else {
			final String cacheName = cacheName( keys[0] );
			final ProtoStreamMappingAdapter mapper = provider.getDataMapperForCache( cacheName );
			final Map<EntityKey,ProtostreamId> keyConversionMatch = new HashMap<>();
			final Set<ProtostreamId> convertedKeys = new HashSet<>();
			for ( EntityKey ek : keys ) {
				if ( ek == null ) {
					continue;
				}
				assert cacheName( ek ).equals( cacheName ) : "The javadoc comment promised batches would be loaded from the same table";
				ProtostreamId idBuffer = mapper.createIdPayload( ek.getColumnNames(), ek.getColumnValues() );
				keyConversionMatch.put( ek, idBuffer );
				convertedKeys.add( idBuffer );
			}
			final Map<ProtostreamId, ProtostreamPayload> loadedBulk = mapper.withinCacheEncodingContext( c -> {
				//TODO getAll doesn't support versioned entries ?!
				return c.getAll( convertedKeys );
			} );

			final List<Tuple> results = new ArrayList<>( keys.length );
			for ( int i = 0; i < keys.length; i++ ) {
				EntityKey originalKey = keys[i];
				if ( originalKey == null ) {
					results.add( null );
					continue;
				}
				ProtostreamId protostreamId = keyConversionMatch.get( originalKey );
				ProtostreamPayload payload = loadedBulk.get( protostreamId );
				if ( payload == null ) {
					results.add( null );
					continue;
				}
				results.add( payload.toTuple( SnapshotType.UNKNOWN ) );
			}
			return results;
		}
	}

	private static boolean associationStoredWithinEntityEntry(AssociationKey key, AssociationContext associationContext) {
		final String cacheName = cacheName( key );
		final String entityTableName = associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata().getEntityKeyMetadata().getTable();

		// in the case of embedded collections, the entityTableName and the cacheName are equal but they reference the name of a
		// join table so the association is not stored in the entity entry.

		return cacheName.equals( entityTableName ) && ! key.getMetadata().getAssociationKind().equals( AssociationKind.EMBEDDED_COLLECTION );
	}

	private static class InfinispanRemoteTuplesSupplier implements TuplesSupplier {

		private final RemoteCache<ProtostreamId, ProtostreamPayload> remoteCache;

		public InfinispanRemoteTuplesSupplier(RemoteCache<ProtostreamId, ProtostreamPayload> remoteCache, String cacheName) {
			this.remoteCache = remoteCache;
		}

		@Override
		public ClosableIterator<Tuple> get(TransactionContext transactionContext) {
			CloseableIterator<Entry<Object, MetadataValue<Object>>> iterator = remoteCache.retrieveEntriesWithMetadata( null, 100 );
			return new InfinispanRemoteTupleIterator( iterator );
		}
	}

	private static class InfinispanRemoteTupleIterator implements ClosableIterator<Tuple> {

		private final CloseableIterator<Entry<Object, MetadataValue<Object>>> iterator;

		public InfinispanRemoteTupleIterator(CloseableIterator<Entry<Object, MetadataValue<Object>>> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public Tuple next() {
			Entry<Object, MetadataValue<Object>> next = iterator.next();
			VersionedTuple tuple = createTuple( next );
			return tuple;
		}

		private VersionedTuple createTuple(Entry<Object, MetadataValue<Object>> next) {
			ProtostreamPayload obj = (ProtostreamPayload) next.getValue().getValue();
			long version = next.getValue().getVersion();
			VersionedTuple tuple = obj.toVersionedTuple( SnapshotType.UPDATE );
			tuple.setVersion( version );
			return tuple;
		}

		@Override
		public void close() {
			iterator.close();
		}
	}
}
