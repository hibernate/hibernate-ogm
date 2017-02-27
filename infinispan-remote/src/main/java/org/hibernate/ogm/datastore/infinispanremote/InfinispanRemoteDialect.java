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
import org.hibernate.ogm.datastore.map.impl.MapAssociationSnapshot;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TuplesSupplier;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
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
public class InfinispanRemoteDialect<EK,AK,ISK> extends BaseGridDialect implements MultigetGridDialect {

	private static final Log log = LoggerFactory.getLogger();

	private final InfinispanRemoteDatastoreProvider provider;

	public InfinispanRemoteDialect(InfinispanRemoteDatastoreProvider provider) {
		this.provider = Objects.requireNonNull( provider );
	}

	@Override
	public Tuple getTuple(EntityKey key, OperationContext operationContext) {
		final String cacheName = key.getTable();
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
	public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) {
		VersionedTuple versionedTuple = (VersionedTuple) tuplePointer.getTuple();
		final String cacheName = key.getTable();
		log.debugf( "insertOrUpdateTuple for key '%s' on cache '%s'", key, cacheName );
		ProtoStreamMappingAdapter mapper = provider.getDataMapperForCache( cacheName );
		ProtostreamPayload valuePayload = mapper.createValuePayload( versionedTuple );
		ProtostreamId idBuffer = mapper.createIdPayload( key.getColumnNames(), key.getColumnValues() );
		boolean optimisticLockFailed;
		if ( versionedTuple.getSnapshotType() == SnapshotType.INSERT ) {
			optimisticLockFailed = null != mapper.withinCacheEncodingContext( c -> c.putIfAbsent( idBuffer, valuePayload ) );
			if ( optimisticLockFailed ) {
				throw new TupleAlreadyExistsException( key ); //TODO add a nicer message?
			}
		}
		else {
			mapper.withinCacheEncodingContext( c -> c.put( idBuffer, valuePayload ) );
		}
		versionedTuple.setSnapshotType( SnapshotType.UPDATE );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		final String cacheName = key.getTable();
		log.debugf( "removeTuple for key '%s' on cache '%s'", key, cacheName );
		ProtoStreamMappingAdapter mapper = provider.getDataMapperForCache( cacheName );
		ProtostreamId idBuffer = mapper.createIdPayload( key.getColumnNames(), key.getColumnValues() );
		mapper.withinCacheEncodingContext( c -> c.remove( idBuffer ) );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		Map<RowKey,Map<String, Object>> results = loadRowKeysByQuery( key );
		return new Association( new MapAssociationSnapshot( results ) );
	}

	private Map<RowKey, Map<String, Object>> loadRowKeysByQuery(AssociationKey key) {
		final String cacheName = key.getTable();
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

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		if ( associationStoredWithinEntityEntry( key, associationContext ) ) {
			insertOrUpdateAssociationEmbeddedInEntity( key, association, associationContext );
		}
		else {
			insertOrUpdateAssociationMappedAsDedicatedEntries( key, association, associationContext );
		}
	}

	private void insertOrUpdateAssociationMappedAsDedicatedEntries(AssociationKey key, Association association, AssociationContext associationContext) {
		final String cacheName = key.getTable();
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

	private void insertOrUpdateAssociationEmbeddedInEntity(AssociationKey key, Association association, AssociationContext associationContext) {
		final String cacheName = key.getTable();
		final ProtoStreamMappingAdapter mapper = provider.getDataMapperForCache( cacheName );
		log.debugf( "insertOrUpdateAssociation for key '%s' on cache '%s', mapped as in-entity foreign keys", key, cacheName );
		final List<AssociationOperation> operations = association.getOperations();
		for ( AssociationOperation ao : operations ) {
			AssociationOperationType type = ao.getType();
			RowKey rowKey = ao.getKey();
			Tuple sourceTuple = ao.getValue();
			ProtostreamId idBuffer = mapper.createIdPayload( rowKey.getColumnNames(), rowKey.getColumnValues() );
			Tuple targetTuple;
			ProtostreamPayload existingPayload = mapper.withinCacheEncodingContext( c -> c.get( idBuffer ) );
			if ( existingPayload == null ) {
				targetTuple = new Tuple();
				targetTuple.setSnapshotType( SnapshotType.INSERT );
			}
			else {
				targetTuple = existingPayload.toTuple( SnapshotType.UPDATE );
			}
			switch ( type ) {
				case PUT:
					for ( String columnName : rowKey.getColumnNames() ) {
						targetTuple.put( columnName, sourceTuple.get( columnName ) );
					}
					ProtostreamPayload valuePayloadForPut = mapper.createValuePayload( targetTuple );
					mapper.withinCacheEncodingContext( c -> c.put( idBuffer, valuePayloadForPut ) );
					break;
				case REMOVE:
					for ( String columnName : key.getColumnNames() ) {
						targetTuple.remove( columnName );
					}
					ProtostreamPayload valuePayloadForRemove = mapper.createValuePayload( targetTuple );
					mapper.withinCacheEncodingContext( c -> c.put( idBuffer, valuePayloadForRemove ) );
					break;
				case CLEAR:
					throw new AssertionFailure( "Request for CLEAR operation on an association mapped as foreign key embedded an an entity. Makes no sense?" );
			}
		}
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		// N.B. 'key' might match multiple entries
		final String cacheName = key.getTable();
		log.debugf( "removeAssociation for key '%s' on cache '%s'", key, cacheName );
		final ProtoStreamMappingAdapter mapper = provider.getDataMapperForCache( cacheName );
		if ( associationStoredWithinEntityEntry( key, associationContext ) ) {
			removeAssociationFromEntity( mapper, key );
		}
		else {
			removeAssociationFromBridgeTable( mapper, key );
		}
	}

	private void removeAssociationFromBridgeTable(ProtoStreamMappingAdapter mapper, AssociationKey key) {
		Map<RowKey, Map<String, Object>> rowsMap = loadRowKeysByQuery( key );
		for ( RowKey rowKey : rowsMap.keySet() ) {
			String[] columnNames = rowKey.getColumnNames();
			Object[] columnValues = rowKey.getColumnValues();
			ProtostreamId idBuffer = mapper.createIdPayload( columnNames, columnValues );
			mapper.withinCacheEncodingContext( c -> c.remove( idBuffer ) ) ;
		}
	}

	private void removeAssociationFromEntity(ProtoStreamMappingAdapter mapper, AssociationKey key) {
		Map<RowKey, Map<String, Object>> rowsMap = loadRowKeysByQuery( key );
		for ( RowKey rowKey : rowsMap.keySet() ) {
			String[] columnNames = rowKey.getColumnNames();
			Object[] columnValues = rowKey.getColumnValues();
			for ( String keyColumn : key.getColumnNames() ) {
				int indexOf = org.hibernate.ogm.util.impl.ArrayHelper.indexOf( columnNames, keyColumn );
				if ( indexOf != -1 ) {
					columnValues[indexOf] = null;
				}
			}
			Tuple updatedPayload = new Tuple();
			for ( int i = 0; i < columnNames.length; i++ ) {
				updatedPayload.put( columnNames[i], columnValues[i] );
			}
			ProtostreamId idBuffer = mapper.createIdPayload( columnNames, columnValues );
			ProtostreamPayload valuePayload = mapper.createValuePayload( updatedPayload );
			mapper.withinCacheEncodingContext( c -> c.put( idBuffer, valuePayload ) );
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
		final String cacheName = entityKeyMetadata.getTable();
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
			final String cacheName = keys[0].getTable();
			final ProtoStreamMappingAdapter mapper = provider.getDataMapperForCache( cacheName );
			final Map<EntityKey,ProtostreamId> keyConversionMatch = new HashMap<>();
			final Set<ProtostreamId> convertedKeys = new HashSet<>();
			for ( EntityKey ek : keys ) {
				if ( ek == null ) {
					continue;
				}
				assert ek.getTable().equals( cacheName ) : "The javadoc comment promised batches would be loaded from the same table";
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
		final String cacheName = key.getTable();
		final String entityTableName = associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata().getEntityKeyMetadata().getTable();
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
