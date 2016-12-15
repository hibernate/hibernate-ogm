/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.datastore.document.impl.DotPatternMapHelpers;
import org.hibernate.ogm.datastore.document.impl.EmbeddableStateFinder;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.spi.AssociationStorageOption;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisAssociation;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisAssociationSnapshot;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisJsonTupleSnapshot;
import org.hibernate.ogm.datastore.redis.dialect.value.Association;
import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.redis.impl.json.JsonEntityStorageStrategy;
import org.hibernate.ogm.dialect.batch.spi.GroupedChangesToEntityOperation;
import org.hibernate.ogm.dialect.batch.spi.GroupingByEntityDialect;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateTupleOperation;
import org.hibernate.ogm.dialect.batch.spi.Operation;
import org.hibernate.ogm.dialect.batch.spi.RemoveAssociationOperation;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TuplesSupplier;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.AssociationType;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.ogm.model.spi.TupleOperation;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.Type;

import com.lambdaworks.redis.KeyScanCursor;
import com.lambdaworks.redis.ScanArgs;

/**
 * Stores tuples and associations inside Redis as JSON.
 * <p>
 * Tuples are stored in Redis as a JSON serialization of a {@link Entity} object. Associations are stored in Redis obtained as a
 * JSON serialization of a {@link Association} object either within the entity or external.
 * See {@link org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties#ASSOCIATIONS_STORE} on how to configure
 * entity or external storage.
 *
 * @author Mark Paluch
 * @author Guillaume Smet
 */
public class RedisJsonDialect extends AbstractRedisDialect implements MultigetGridDialect, GroupingByEntityDialect {

	protected final JsonEntityStorageStrategy entityStorageStrategy;

	public RedisJsonDialect(RedisDatastoreProvider provider) {
		super( provider.getConnection(), provider.isCluster() );
		this.entityStorageStrategy = new JsonEntityStorageStrategy( strategy, connection );
	}

	@Override
	public GridType overrideType(Type type) {
		return strategy.overrideType( type );
	}

	@Override
	public Tuple getTuple(EntityKey key, OperationContext operationContext) {
		Entity entity = entityStorageStrategy.getEntity( entityId( key ) );

		if ( entity != null ) {
			return new Tuple( new RedisJsonTupleSnapshot( entity ), SnapshotType.UPDATE );
		}
		else if ( isInTheInsertionQueue( key, operationContext ) ) {
			return createTuple( key, operationContext );
		}
		else {
			return null;
		}
	}

	@Override
	public Tuple createTuple(EntityKey key, OperationContext operationContext) {
		return new Tuple( new RedisJsonTupleSnapshot( new Entity() ), SnapshotType.INSERT );
	}

	@Override
	public void executeGroupedChangesToEntity(GroupedChangesToEntityOperation groupedOperation) {
		Entity owningEntity = null;
		List<AssociationKey> associationsToRemove = new ArrayList<>();
		OptionsContext optionsContext = null;

		for ( Operation operation : groupedOperation.getOperations() ) {
			if ( operation instanceof InsertOrUpdateTupleOperation ) {
				InsertOrUpdateTupleOperation insertOrUpdateTupleOperation = (InsertOrUpdateTupleOperation) operation;
				EntityKey key = insertOrUpdateTupleOperation.getEntityKey();
				Tuple tuple = insertOrUpdateTupleOperation.getTuplePointer().getTuple();
				TupleContext tupleContext = insertOrUpdateTupleOperation.getTupleContext();

				if ( owningEntity == null ) {
					owningEntity = getEntityFromTuple( tuple );
				}

				EmbeddableStateFinder embeddableStateFinder = new EmbeddableStateFinder( tuple, tupleContext );

				for ( TupleOperation tupleOperation : tuple.getOperations() ) {
					String column = tupleOperation.getColumn();
					if ( key.getMetadata().isKeyColumn( column ) ) {
						continue;
					}
					switch ( tupleOperation.getType() ) {
					case PUT:
						owningEntity.set( column, tupleOperation.getValue() );
						break;
					case PUT_NULL:
					case REMOVE:
						// try and find if this column is within an embeddable and if that embeddable is null
						// if true, unset the full embeddable
						String nullEmbeddable = embeddableStateFinder.getOuterMostNullEmbeddableIfAny( column );
						if ( nullEmbeddable != null ) {
							// we have a null embeddable
							owningEntity.unset( nullEmbeddable );
						}
						else {
							// simply unset the column
							owningEntity.unset( column );
						}
						break;
					}
				}

				tuple.setSnapshotType( SnapshotType.UPDATE );

				optionsContext = tupleContext.getTupleTypeContext().getOptionsContext();
			}
			else if ( operation instanceof InsertOrUpdateAssociationOperation ) {
				InsertOrUpdateAssociationOperation insertOrUpdateAssociationOperation = (InsertOrUpdateAssociationOperation) operation;
				AssociationKey associationKey = insertOrUpdateAssociationOperation.getAssociationKey();
				org.hibernate.ogm.model.spi.Association association = insertOrUpdateAssociationOperation.getAssociation();
				AssociationContext associationContext = insertOrUpdateAssociationOperation.getContext();

				RedisAssociation redisAssociation = ( (RedisAssociationSnapshot) association.getSnapshot() ).getRedisAssociation();
				Object rows = getAssociationRows( association, associationKey, associationContext );

				redisAssociation.setRows( rows );

				if ( isStoredInEntityStructure( associationKey.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
					if ( owningEntity == null ) {
						owningEntity = (Entity) redisAssociation.getOwningDocument();
						optionsContext = associationContext.getAssociationTypeContext().getOwnerEntityOptionsContext();
					}
				}
				else {
					// We don't want to remove the association anymore as it's superseded by an update
					associationsToRemove.remove( associationKey );

					String associationId = associationId( associationKey );
					Long ttl = getObjectTTL( associationId, associationContext.getAssociationTypeContext().getOptionsContext() );
					storeAssociation( associationKey, (Association) redisAssociation.getOwningDocument() );
					setObjectTTL( associationId, ttl );
				}
			}
			else if ( operation instanceof RemoveAssociationOperation ) {
				RemoveAssociationOperation removeAssociationOperation = (RemoveAssociationOperation) operation;
				AssociationKey associationKey = removeAssociationOperation.getAssociationKey();
				AssociationContext associationContext = removeAssociationOperation.getContext();

				if ( isStoredInEntityStructure( associationKey.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
					if ( owningEntity == null ) {
						TuplePointer tuplePointer = getEmbeddingEntityTuplePointer( associationKey, associationContext );
						owningEntity = getEntityFromTuple( tuplePointer.getTuple() );
					}
					if ( owningEntity != null ) {
						owningEntity.unset( associationKey.getMetadata().getCollectionRole() );
						optionsContext = associationContext.getAssociationTypeContext().getOwnerEntityOptionsContext();
					}
				}
				else {
					associationsToRemove.add( associationKey );
				}
			}
			else {
				throw new IllegalStateException( operation.getClass().getSimpleName() + " not supported here" );
			}
		}

		if ( owningEntity != null ) {
			storeEntity( groupedOperation.getEntityKey(), owningEntity, optionsContext );
		}
		if ( associationsToRemove.size() > 0 ) {
			removeAssociations( associationsToRemove );
		}
	}

	@Override
	public boolean isStoredInEntityStructure(
			AssociationKeyMetadata keyMetadata,
			AssociationTypeContext associationTypeContext) {

		AssociationStorageType associationStorage = getAssociationStorageType( associationTypeContext );

		if ( keyMetadata.getAssociationType() == AssociationType.ONE_TO_ONE || keyMetadata.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION || associationStorage == AssociationStorageType.IN_ENTITY ) {
			return true;
		}

		return false;
	}

	private AssociationStorageType getAssociationStorageType(AssociationTypeContext associationTypeContext) {
		return associationTypeContext.getOptionsContext().getUnique(
				AssociationStorageOption.class
		);
	}

	@Override
	public org.hibernate.ogm.model.spi.Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		RedisAssociation redisAssociation = null;

		if ( isStoredInEntityStructure( key.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
			TuplePointer tuplePointer = getEmbeddingEntityTuplePointer( key, associationContext );
			if ( tuplePointer == null ) {
				// The entity associated with this association has already been removed
				// see ManyToOneTest#testRemovalOfTransientEntityWithAssociation
				return null;
			}
			Entity owningEntity = getEntityFromTuple( tuplePointer.getTuple() );

			if ( owningEntity != null && DotPatternMapHelpers.hasField(
					owningEntity.getPropertiesAsHierarchy(),
					key.getMetadata().getCollectionRole()
			) ) {
				redisAssociation = RedisAssociation.fromEmbeddedAssociation( tuplePointer, key.getMetadata() );
			}
		}
		else {
			Association association = getAssociation( key );
			if ( association != null ) {
				redisAssociation = RedisAssociation.fromAssociationDocument( association );
			}
		}

		return redisAssociation != null ? new org.hibernate.ogm.model.spi.Association(
				new RedisAssociationSnapshot(
						redisAssociation, key
				)
		) : null;
	}

	@Override
	public org.hibernate.ogm.model.spi.Association createAssociation(
			AssociationKey key,
			AssociationContext associationContext) {
		RedisAssociation redisAssociation;

		if ( isStoredInEntityStructure( key.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
			TuplePointer tuplePointer = getEmbeddingEntityTuplePointer( key, associationContext );
			Entity owningEntity = getEntityFromTuple( tuplePointer.getTuple() );

			if ( owningEntity == null ) {
				owningEntity = new Entity();
				storeEntity( key.getEntityKey(), owningEntity, associationContext.getAssociationTypeContext().getOwnerEntityOptionsContext() );
				tuplePointer.setTuple( new Tuple( new RedisJsonTupleSnapshot( owningEntity ), SnapshotType.UPDATE ) );
			}

			redisAssociation = RedisAssociation.fromEmbeddedAssociation( tuplePointer, key.getMetadata() );
		}
		else {
			redisAssociation = RedisAssociation.fromAssociationDocument( new Association() );
		}

		org.hibernate.ogm.model.spi.Association association = new org.hibernate.ogm.model.spi.Association(
				new RedisAssociationSnapshot(
						redisAssociation,
						key
				)
		);

		// in the case of an association stored in the entity structure, we might end up with rows present in the current snapshot of the entity
		// while we want an empty association here. So, in this case, we clear the snapshot to be sure the association created is empty.
		if ( !association.isEmpty() ) {
			association.clear();
		}

		return association;
	}

	/**
	 * Returns the rows of the given association as to be stored in the database. Elements of the returned list are
	 * either
	 * <ul>
	 * <li>plain values such as {@code String}s, {@code int}s etc. in case there is exactly one row key column which is
	 * not part of the association key (in this case we don't need to persist the key name as it can be restored from
	 * the association key upon loading) or</li>
	 * <li>{@code Entity}s with keys/values for all row key columns which are not part of the association key</li>
	 * </ul>
	 */
	protected Object getAssociationRows(
			org.hibernate.ogm.model.spi.Association association,
			AssociationKey key,
			AssociationContext associationContext) {

		boolean organizeByRowKey = DotPatternMapHelpers.organizeAssociationMapByRowKey(
				association,
				key,
				associationContext
		);

		// only in-entity maps can be mapped by row key to prevent huge external association maps
		if ( isStoredInEntityStructure(
				key.getMetadata(),
				associationContext.getAssociationTypeContext()
		) && organizeByRowKey ) {
			String rowKeyColumn = key.getMetadata().getRowKeyIndexColumnNames()[0];
			Map<String, Object> rows = new HashMap<>();

			for ( RowKey rowKey : association.getKeys() ) {
				Map<String, Object> row = (Map<String, Object>) getAssociationRow( association.get( rowKey ), key );

				String rowKeyValue = (String) row.remove( rowKeyColumn );

				// if there is a single column on the value side left, unwrap it
				if ( row.keySet().size() == 1 ) {
					rows.put( rowKeyValue, row.values().iterator().next() );
				}
				else {
					rows.put( rowKeyValue, row );
				}
			}

			return rows;
		}

		List<Object> rows = new ArrayList<>( association.size() );
		for ( RowKey rowKey : association.getKeys() ) {
			rows.add( getAssociationRow( association.get( rowKey ), key ) );
		}

		return rows;
	}

	@Override
	public void forEachTuple(final ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata) {
		KeyScanCursor<String> cursor = null;
		String prefix = entityKeyMetadata.getTable() + ":";

		ScanArgs scanArgs = ScanArgs.Builder.matches( prefix + "*" );
		do {
			cursor = scan( cursor, scanArgs );
			consumer.consume( new RedisJsonDialectTuplesSupplier( cursor, entityStorageStrategy, prefix, entityKeyMetadata ) );
		} while ( !cursor.isFinished() );
	}

	private class RedisJsonDialectTuplesSupplier implements TuplesSupplier {

		private final KeyScanCursor<String> cursor;
		private final String prefix;
		private final EntityKeyMetadata entityKeyMetadata;
		private final JsonEntityStorageStrategy storageStrategy;

		public RedisJsonDialectTuplesSupplier(KeyScanCursor<String> cursor, JsonEntityStorageStrategy storageStrategy, String prefix, EntityKeyMetadata entityKeyMetadata) {
			this.cursor = cursor;
			this.storageStrategy = storageStrategy;
			this.prefix = prefix;
			this.entityKeyMetadata = entityKeyMetadata;
		}

		@Override
		public ClosableIterator<Tuple> get(TransactionContext transactionContext) {
			return new RedisJsonTupleIterator( cursor, storageStrategy, prefix, entityKeyMetadata );
		}
	}

	private class RedisJsonTupleIterator implements ClosableIterator<Tuple> {

		private final Iterator<String> iterator;
		private final EntityKeyMetadata entityKeyMetadata;
		private final String prefix;
		private final JsonEntityStorageStrategy storageStrategy;

		public RedisJsonTupleIterator(KeyScanCursor<String> cursor, JsonEntityStorageStrategy storageStrategy, String prefix, EntityKeyMetadata entityKeyMetadata) {
			this.storageStrategy = storageStrategy;
			this.prefix = prefix;
			this.entityKeyMetadata = entityKeyMetadata;
			this.iterator = cursor.getKeys().iterator();
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public Tuple next() {
			String key = iterator.next();
			Entity document = storageStrategy.getEntity( key );
			addKeyValuesFromKeyName( entityKeyMetadata, prefix, key, document );
			return createTuple( document );
		}

		private Tuple createTuple(Entity document) {
			return new Tuple( new RedisJsonTupleSnapshot( document ), SnapshotType.UPDATE );
		}

		@Override
		public void close() {
		}
	}

	private void storeEntity(EntityKey key, Entity entity, OptionsContext optionsContext) {
		String entityId = entityId( key );
		Long currentTtl = getObjectTTL( entityId, optionsContext );

		entityStorageStrategy.storeEntity( entityId, entity );

		setObjectTTL( entityId, currentTtl );
	}

	public JsonEntityStorageStrategy getEntityStorageStrategy() {
		return entityStorageStrategy;
	}

	private Entity getEntityFromTuple(Tuple tuple) {
		if ( tuple == null ) {
			return null;
		}
		return ( (RedisJsonTupleSnapshot) tuple.getSnapshot() ).getEntity();
	}


	// MultigetGridDialect

	@Override
	public List<Tuple> getTuples(EntityKey[] keys, TupleContext tupleContext) {
		if ( keys.length == 0 ) {
			return Collections.emptyList();
		}

		String ids[] = new String[keys.length];

		for ( int i = 0; i < keys.length; i++ ) {
			ids[i] = entityId( keys[i] );
		}

		Iterable<Entity> entities = entityStorageStrategy.getEntities( ids );
		List<Tuple> tuples = new ArrayList<>( keys.length );

		int i = 0;
		for ( Entity entity : entities ) {
			if ( entity != null ) {
				EntityKey key = keys[i];
				addIdToEntity( entity, key.getColumnNames(), key.getColumnValues() );
				tuples.add( new Tuple( new RedisJsonTupleSnapshot( entity ), SnapshotType.UPDATE ) );
			}
			else {
				tuples.add( null );
			}
			i++;
		}

		return tuples;
	}
}
