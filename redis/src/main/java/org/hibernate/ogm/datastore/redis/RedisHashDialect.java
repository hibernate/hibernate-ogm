/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisAssociation;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisAssociationSnapshot;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisHashTupleSnapshot;
import org.hibernate.ogm.datastore.redis.dialect.value.HashEntity;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.redis.impl.hash.RedisHashTypeConverter;
import org.hibernate.ogm.dialect.batch.spi.GroupedChangesToEntityOperation;
import org.hibernate.ogm.dialect.batch.spi.GroupingByEntityDialect;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateTupleOperation;
import org.hibernate.ogm.dialect.batch.spi.Operation;
import org.hibernate.ogm.dialect.batch.spi.RemoveAssociationOperation;
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
import org.hibernate.ogm.model.key.spi.AssociationType;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.ogm.model.spi.TupleOperation;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.Type;

import com.lambdaworks.redis.KeyScanCursor;
import com.lambdaworks.redis.ScanArgs;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;

/**
 * Stores tuples and associations inside Redis using hash data structures.
 * <p>
 * Tuples are stored in Redis within hashes. Associations are stored in Redis obtained as either single values or a
 * JSON serialization of a {@link org.hibernate.ogm.datastore.redis.dialect.value.Association} object in list/set data structures.
 *
 * @author Mark Paluch
 * @author Guillaume Smet
 */
public class RedisHashDialect extends AbstractRedisDialect implements GroupingByEntityDialect {

	public RedisHashDialect(RedisDatastoreProvider provider) {
		super( provider.getConnection(), provider.isCluster() );
	}

	@Override
	public GridType overrideType(Type type) {
		return RedisHashTypeConverter.INSTANCE.convert( type );
	}

	@Override
	public Tuple getTuple(EntityKey key, OperationContext operationContext) {
		String entityIdString = entityId( key );
		if ( !connection.exists( entityIdString ) ) {
			return null;
		}

		Map<String, String> objects;
		if ( operationContext.getTupleTypeContext().getSelectableColumns().isEmpty() ) {
			objects = connection.hgetall( entityIdString );
		}
		else {
			List<String> hmget = connection.hmget( entityIdString, getFields( operationContext.getTupleTypeContext() ) );
			objects = toEntity( operationContext.getTupleTypeContext(), hmget );
		}

		return createTuple( objects );
	}

	@Override
	public Tuple createTuple(EntityKey key, OperationContext operationContext) {
		return new Tuple( new RedisHashTupleSnapshot( new HashEntity( new HashMap<String, String>() ) ), SnapshotType.INSERT );
	}

	private Map<String, String> toEntity(TupleTypeContext tupleTypeContext, List<String> hmget) {
		Map<String, String> objects = new HashMap<>();
		for ( int i = 0; i < tupleTypeContext.getSelectableColumns().size(); i++ ) {
			String columnName = tupleTypeContext.getSelectableColumns().get( i );
			String value = hmget.get( i );
			if ( value == null ) {
				continue;
			}
			objects.put( columnName, value );
		}
		return objects;
	}

	private String[] getFields(TupleTypeContext tupleTypeContext) {
		return tupleTypeContext.getSelectableColumns().toArray( new String[tupleTypeContext.getSelectableColumns().size()] );
	}

	@Override
	public Association getAssociation(
			AssociationKey key, AssociationContext associationContext) {
		RedisAssociation redisAssociation = null;

		if ( isStoredInEntityStructure( key.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
			TuplePointer tuplePointer = getEmbeddingEntityTuplePointer( key, associationContext );
			if ( tuplePointer == null ) {
				// The entity associated with this association has already been removed
				// see ManyToOneTest#testRemovalOfTransientEntityWithAssociation
				return null;
			}
			HashEntity owningEntity = getEntityFromTuple( tuplePointer.getTuple() );

			if ( owningEntity != null && owningEntity.has( key.getMetadata().getCollectionRole() ) ) {
				redisAssociation = RedisAssociation.fromHashEmbeddedAssociation( tuplePointer, key.getMetadata() );
			}
		}
		else {
			org.hibernate.ogm.datastore.redis.dialect.value.Association association = getAssociation( key );

			if ( association == null ) {
				return null;
			}
			redisAssociation = RedisAssociation.fromAssociationDocument( association );
		}

		return redisAssociation != null ? new org.hibernate.ogm.model.spi.Association(
				new RedisAssociationSnapshot(
						redisAssociation, key
				)
		) : null;
	}

	@Override
	public Association createAssociation(
			AssociationKey key, AssociationContext associationContext) {

		RedisAssociation redisAssociation;
		if ( isStoredInEntityStructure( key.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
			TuplePointer tuplePointer = getEmbeddingEntityTuplePointer( key, associationContext );
			HashEntity owningEntity = getEntityFromTuple( tuplePointer.getTuple() );

			if ( owningEntity == null ) {
				owningEntity = new HashEntity( new HashMap<String, String>() );
				storeEntity( key.getEntityKey(), owningEntity, associationContext.getAssociationTypeContext().getOwnerEntityOptionsContext() );
				tuplePointer.setTuple( new Tuple( new RedisHashTupleSnapshot( owningEntity ), SnapshotType.UPDATE ) );
			}

			redisAssociation = RedisAssociation.fromHashEmbeddedAssociation( tuplePointer, key.getMetadata() );

		}
		else {
			redisAssociation = RedisAssociation.fromAssociationDocument( new org.hibernate.ogm.datastore.redis.dialect.value.Association() );
		}

		return new org.hibernate.ogm.model.spi.Association(
				new RedisAssociationSnapshot(
						redisAssociation, key
				)
		);
	}

	private Object getAssociationRows(
			Association association,
			AssociationKey key) {
		List<Object> rows = new ArrayList<>( association.size() );
		for ( RowKey rowKey : association.getKeys() ) {
			rows.add( getAssociationRow( association.get( rowKey ), key ) );
		}

		return rows;
	}

	@Override
	public boolean isStoredInEntityStructure(
			AssociationKeyMetadata keyMetadata, AssociationTypeContext associationTypeContext) {
		if ( keyMetadata.getAssociationType() == AssociationType.ONE_TO_ONE ) {
			return true;
		}
		return false;
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata) {
		KeyScanCursor<String> cursor = null;
		String prefix = entityKeyMetadata.getTable() + ":";

		ScanArgs scanArgs = ScanArgs.Builder.matches( prefix + "*" );
		do {
			cursor = scan( cursor, scanArgs );
			consumer.consume( new RedisHashDialectTuplesSupplier( cursor, connection, prefix, entityKeyMetadata ) );
		} while ( !cursor.isFinished() );
	}

	private static Tuple createTuple(Map<String, String> properties) {
		return new Tuple( new RedisHashTupleSnapshot( new HashEntity( properties ) ), SnapshotType.UPDATE );
	}

	protected void addKeyValuesFromKeyName(
			EntityKeyMetadata entityKeyMetadata,
			String prefix,
			String key,
			Map<String, String> document) {
		if ( key.startsWith( prefix ) ) {

			String keyWithoutPrefix = key.substring( prefix.length() );

			Map<String, String> keys = keyToMap( entityKeyMetadata, keyWithoutPrefix );

			for ( Map.Entry<String, String> entry : keys.entrySet() ) {
				document.put( entry.getKey(), entry.getValue() );
			}
		}
	}

	@Override
	public void executeGroupedChangesToEntity(GroupedChangesToEntityOperation groupedOperation) {
		String entityId = entityId( groupedOperation.getEntityKey() );
		HashEntity owningEntity = null;
		List<String> toDelete = new ArrayList<String>();
		List<AssociationKey> associationsToRemove = new ArrayList<AssociationKey>();
		OptionsContext optionsContext = null;

		for ( Operation operation : groupedOperation.getOperations() ) {
			if ( operation instanceof InsertOrUpdateTupleOperation ) {
				InsertOrUpdateTupleOperation insertOrUpdateTupleOperation = (InsertOrUpdateTupleOperation) operation;
				Tuple tuple = insertOrUpdateTupleOperation.getTuplePointer().getTuple();
				TupleContext tupleContext = insertOrUpdateTupleOperation.getTupleContext();

				if ( owningEntity == null ) {
					owningEntity = getEntityFromTuple( tuple );
				}

				for ( TupleOperation action : tuple.getOperations() ) {
					switch ( action.getType() ) {
						case PUT:
							// TODO: IllegalStateException when value is not String/Character?
							if ( action.getValue() instanceof Character ) {
								owningEntity.set( action.getColumn(), action.getValue().toString() );
							}
							else {
								owningEntity.set( action.getColumn(), (String) action.getValue() );
							}
							toDelete.remove( action.getColumn() );
							break;
						case REMOVE:
						case PUT_NULL:
							owningEntity.unset( action.getColumn() );
							toDelete.add( action.getColumn() );
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

				Object rows = getAssociationRows( association, associationKey );

				RedisAssociation redisAssociation = ( (RedisAssociationSnapshot) association.getSnapshot() ).getRedisAssociation();
				redisAssociation.setRows( rows );

				if ( isStoredInEntityStructure(
						associationKey.getMetadata(),
						associationContext.getAssociationTypeContext()
				) ) {
					if ( owningEntity == null ) {
						owningEntity = (HashEntity) redisAssociation.getOwningDocument();
						optionsContext = associationContext.getAssociationTypeContext().getOwnerEntityOptionsContext();
					}
				}
				else {
					// We don't want to remove the association anymore as it's superseded by an update
					associationsToRemove.remove( associationKey );

					String associationId = associationId( associationKey );
					Long ttl = getObjectTTL( associationId, associationContext.getAssociationTypeContext().getOptionsContext() );
					storeAssociation(
							associationKey,
							(org.hibernate.ogm.datastore.redis.dialect.value.Association) redisAssociation.getOwningDocument()
					);
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
					toDelete.add( associationKey.getMetadata().getCollectionRole() );
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
		if ( !toDelete.isEmpty() ) {
			connection.hdel( entityId, toDelete.toArray( new String[toDelete.size()] ) );
		}
		if ( associationsToRemove.size() > 0 ) {
			removeAssociations( associationsToRemove );
		}
	}

	private HashEntity getEntityFromTuple(Tuple tuple) {
		if ( tuple == null ) {
			return null;
		}
		return ( (RedisHashTupleSnapshot) tuple.getSnapshot() ).getEntity();
	}

	private void storeEntity(EntityKey key, HashEntity entity, OptionsContext optionsContext) {
		String entityId = entityId( key );

		if ( !entity.isEmpty() ) {
			Long currentTtl = getObjectTTL( entityId, optionsContext );
			connection.hmset( entityId, entity.getProperties() );
			setObjectTTL( entityId, currentTtl );
		}
	}

	private class RedisHashDialectTuplesSupplier implements TuplesSupplier {

		private final KeyScanCursor<String> cursor;
		private final RedisClusterCommands<String, String> connection;
		private final String prefix;
		private final EntityKeyMetadata entityKeyMetadata;

		public RedisHashDialectTuplesSupplier(KeyScanCursor<String> cursor, RedisClusterCommands<String, String> connection, String prefix, EntityKeyMetadata entityKeyMetadata) {
			this.cursor = cursor;
			this.connection = connection;
			this.prefix = prefix;
			this.entityKeyMetadata = entityKeyMetadata;
		}

		@Override
		public ClosableIterator<Tuple> get(TransactionContext transactionContext) {
			return new RedisHashTupleIterator( cursor, connection, prefix, entityKeyMetadata );
		}
	}

	private class RedisHashTupleIterator implements ClosableIterator<Tuple> {

		private final Iterator<String> iterator;
		private final EntityKeyMetadata entityKeyMetadata;
		private final RedisClusterCommands<String, String> connection;
		private final String prefix;

		public RedisHashTupleIterator(KeyScanCursor<String> cursor, RedisClusterCommands<String, String> connection, String prefix, EntityKeyMetadata entityKeyMetadata) {
			this.connection = connection;
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
			Map<String, String> hgetall = connection.hgetall( key );
			Map<String, String> properties = new HashMap<>();
			properties.putAll( hgetall );
			addKeyValuesFromKeyName( entityKeyMetadata, prefix, key, properties );
			return createTuple( properties );
		}

		@Override
		public void close() {
		}
	}
}
