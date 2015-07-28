/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.map.impl.MapHelpers;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisAssociation;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisAssociationSnapshot;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisTupleSnapshot;
import org.hibernate.ogm.datastore.redis.dialect.value.Association;
import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.datastore.redis.impl.EntityStorageStrategy;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.redis.impl.hash.ExperimentalHashEntityStorageStrategy;
import org.hibernate.ogm.datastore.redis.impl.json.JsonEntityStorageStrategy;
import org.hibernate.ogm.datastore.redis.impl.json.JsonSerializationStrategy;
import org.hibernate.ogm.datastore.redis.options.EntityStorageType;
import org.hibernate.ogm.datastore.redis.options.impl.AssociationStorageOption;
import org.hibernate.ogm.datastore.redis.options.impl.EntityStorageOption;
import org.hibernate.ogm.datastore.redis.options.impl.TTLOption;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
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
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationKind;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

import com.lambdaworks.redis.KeyScanCursor;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.ScanArgs;
import com.lambdaworks.redis.protocol.LettuceCharsets;

/**
 * Stores tuples and associations inside Redis.
 * <p/>
 * Tuples are stored in Redis as a JSON serialization of a {@link Entity} object. Associations are stored in Redis obtained as a
 * JSON serialization of a {@link Association} object either within the entity or external. See {@link RedisProperties#ASSOCIATIONS_STORE} on how to configure
 * entity or external storage.
 *
 * @author Mark Paluch
 */
public class RedisDialect extends BaseGridDialect {

	public static final String IDENTIFIERS = "Identifiers";
	public static final String ASSOCIATIONS = "Associations";

	protected final Map<EntityStorageType, EntityStorageStrategy> entityStorageStrategies;

	private final RedisDatastoreProvider provider;
	private final RedisConnection<byte[], byte[]> connection;

	private final JsonSerializationStrategy serializationStrategy = new JsonSerializationStrategy();

	public RedisDialect(RedisDatastoreProvider provider) {
		this.provider = provider;
		this.connection = provider.getConnection();

		Map<EntityStorageType, EntityStorageStrategy> strategies = new HashMap<>();
		strategies.put( EntityStorageType.JSON, new JsonEntityStorageStrategy( serializationStrategy, connection ) );
		strategies.put( EntityStorageType.HASH, new ExperimentalHashEntityStorageStrategy( connection ) );

		this.entityStorageStrategies = Collections.unmodifiableMap( strategies );

	}

	/**
	 * Redis essentially has no workable lock strategy
	 *
	 * @param lockable The persister for the entity to be locked.
	 * @param lockMode The type of lock to be acquired.
	 *
	 * @return always null
	 */
	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		// Redis essentially has no workable lock strategy
		return null;
	}

	@Override
	public GridType overrideType(Type type) {
		return serializationStrategy.overrideType( type );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {

		Entity entity = getEntity( key, tupleContext.getOptionsContext() );
		if ( entity != null ) {
			return new Tuple( new RedisTupleSnapshot( entity.getProperties() ) );
		}
		else if ( isInTheQueue( key, tupleContext ) ) {
			// The key has not been inserted in the db but it is in the queue
			return new Tuple( new RedisTupleSnapshot( entity.getProperties() ) );
		}
		else {
			return null;
		}
	}

	private boolean isInTheQueue(EntityKey key, TupleContext tupleContext) {
		OperationsQueue queue = tupleContext.getOperationsQueue();
		return queue != null && queue.contains( key );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		// TODO we don't verify that it does not yet exist assuming that this has been done before by the calling code
		return new Tuple( new RedisTupleSnapshot( new HashMap<String, Object>() ) );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		Map<String, Object> map = ( (RedisTupleSnapshot) tuple.getSnapshot() ).getMap();
		MapHelpers.applyTupleOpsOnMap( tuple, map );
		storeEntity( key, map, tupleContext.getOptionsContext() );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		remove( key );
	}

	@Override
	public boolean isStoredInEntityStructure(
			AssociationKeyMetadata keyMetadata,
			AssociationTypeContext associationTypeContext) {

		AssociationStorageType associationStorage = getAssociationStorageType( associationTypeContext );

		if ( keyMetadata.isOneToOne() || keyMetadata.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION || associationStorage == AssociationStorageType.IN_ENTITY ) {
			return true;
		}

		return false;
	}

	private AssociationStorageType getAssociationStorageType(AssociationTypeContext associationTypeContext) {
		return associationTypeContext.getOptionsContext().getUnique(
				AssociationStorageOption.class
		);
	}


	private Long getTTL(OptionsContext optionsContext) {
		return optionsContext.getUnique( TTLOption.class );
	}

	private Long getTTL(AssociationContext associationContext) {
		return associationContext.getAssociationTypeContext().getOptionsContext().getUnique( TTLOption.class );
	}

	protected EntityStorageStrategy getEntityStorageStrategy(OptionsContext optionsContext) {
		EntityStorageType entitiyStorage = optionsContext.getUnique(
				EntityStorageOption.class
		);

		return getEntityStorageStrategy( entitiyStorage );
	}

	public EntityStorageStrategy getEntityStorageStrategy(EntityStorageType entitiyStorage) {
		return entityStorageStrategies.get( entitiyStorage );
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		byte[] key = identifierId( request.getKey() );
		byte[] hget = connection.get( key );

		if ( hget == null || hget.length == 0 ) {
			connection.set( key, toBytes( Long.toString( request.getInitialValue() ) ) );
			return request.getInitialValue();
		}

		return connection.incrby( key, request.getIncrement() );
	}

	@Override
	public void forEachTuple(final ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {


		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {

			KeyScanCursor<byte[]> cursor = null;
			String pattern = entityKeyMetadata.getTable() + ":*";
			do {

				if ( cursor != null ) {
					cursor = connection.scan( cursor, ScanArgs.Builder.matches( pattern ) );
				}
				else {
					cursor = connection.scan( ScanArgs.Builder.matches( pattern ) );
				}

				for ( byte[] key : cursor.getKeys() ) {

					String type = connection.type( key );
					EntityStorageStrategy entityStorageStrategy;
					if ( "hash".equalsIgnoreCase( type ) ) {
						entityStorageStrategy = entityStorageStrategies.get( EntityStorageType.HASH );
					}
					else {
						entityStorageStrategy = entityStorageStrategies.get( EntityStorageType.JSON );
					}

					Entity document = entityStorageStrategy.getEntity( key );

					// ModelConsumer should not do blocking things.
					consumer.consume( new Tuple( new RedisTupleSnapshot( document.getProperties() ) ) );
				}

			} while ( !cursor.isFinished() );
		}

	}

	@Override
	public org.hibernate.ogm.model.spi.Association getAssociation(
			AssociationKey key,
			AssociationContext associationContext) {
		RedisAssociation redisAssociation = null;

		if ( isStoredInEntityStructure( key.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
			Entity owningEntity = getEntity(
					key.getEntityKey(),
					associationContext
			);

			if ( owningEntity != null && owningEntity.getProperties().containsKey(
					key.getMetadata()
							.getCollectionRole()
			) ) {
				redisAssociation = RedisAssociation.fromEmbeddedAssociation( owningEntity, key.getMetadata() );
			}
		}
		else {
			Association association = getAssociation( key.getEntityKey() );
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
		RedisAssociation redisAssociation = null;

		if ( isStoredInEntityStructure( key.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
			Entity owningEntity = getEntity(
					key.getEntityKey(),
					associationContext
			);
			if ( owningEntity == null ) {
				owningEntity = storeEntity( key.getEntityKey(), new Entity(), associationContext );
			}

			redisAssociation = RedisAssociation.fromEmbeddedAssociation( owningEntity, key.getMetadata() );
		}
		else {
			Association association = new Association();
			redisAssociation = RedisAssociation.fromAssociationDocument( association );
		}

		return new org.hibernate.ogm.model.spi.Association( new RedisAssociationSnapshot( redisAssociation, key ) );
	}

	@Override
	public void insertOrUpdateAssociation(
			AssociationKey associationKey, org.hibernate.ogm.model.spi.Association association,
			AssociationContext associationContext) {
		List<Object> rows = getAssociationRows( association, associationKey );

		RedisAssociation redisAssociation = ( (RedisAssociationSnapshot) association.getSnapshot() ).getRedisAssociation();
		redisAssociation.setRows( rows );

		if ( isStoredInEntityStructure(
				associationKey.getMetadata(),
				associationContext.getAssociationTypeContext()
		) ) {
			storeEntity(
					associationKey.getEntityKey(),
					(Entity) redisAssociation.getOwningDocument(),
					associationContext
			);
		}
		else {
			storeAssociation( associationKey.getEntityKey(), (Association) redisAssociation.getOwningDocument() );
			Long ttl = getTTL( associationContext );
			if ( ttl != null ) {
				expireAssociation( associationKey.getEntityKey(), ttl );
			}
		}

	}


	private List<Object> getAssociationRows(
			org.hibernate.ogm.model.spi.Association association,
			AssociationKey associationKey) {
		List<Object> rows = new ArrayList<Object>( association.size() );

		for ( RowKey rowKey : association.getKeys() ) {
			Tuple tuple = association.get( rowKey );

			String[] columnsToPersist = associationKey.getMetadata()
					.getColumnsWithoutKeyColumns( tuple.getColumnNames() );

			// return value itself if there is only a single column to store
			if ( columnsToPersist.length == 1 ) {
				Object row = tuple.get( columnsToPersist[0] );
				rows.add( row );
			}
			else {
				Map<String, Object> row = new HashMap<String, Object>( columnsToPersist.length );
				for ( String columnName : columnsToPersist ) {
					Object value = tuple.get( columnName );
					if ( value != null ) {
						row.put( columnName, value );
					}
				}

				rows.add( row );
			}
		}
		return rows;
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		if ( isStoredInEntityStructure( key.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
			Entity owningEntity = getEntity(
					key.getEntityKey(),
					associationContext
			);
			if ( owningEntity != null ) {
				owningEntity.removeAssociation( key.getMetadata().getCollectionRole() );
				storeEntity( key.getEntityKey(), owningEntity, associationContext );
			}
		}
		else {
			removeAssociation( key.getEntityKey() );
		}
	}

	public Entity getEntity(EntityKey key, OptionsContext optionsContext) {
		return getEntityStorageStrategy( optionsContext ).getEntity( entityId( key ) );
	}

	public void storeEntity(EntityKey key, Map<String, Object> map, OptionsContext optionsContext) {
		Entity entityDocument = new Entity();
		Set<String> keys = new HashSet<>( Arrays.asList( key.getColumnNames() ) );


		for ( Map.Entry<String, Object> entry : map.entrySet() ) {
			if ( keys.contains( entry.getKey() ) ) {
				continue;
			}
			entityDocument.set( entry.getKey(), entry.getValue() );
		}


		storeEntity( key, entityDocument, optionsContext );

		Long ttl = getTTL( optionsContext );
		if ( ttl != null ) {
			expireEntity( key, ttl );
		}
	}

	public void storeEntity(EntityKey key, Entity document, OptionsContext optionsContext) {
		getEntityStorageStrategy( optionsContext ).storeEntity( entityId( key ), document );
	}

	public Association getAssociation(EntityKey key) {

		byte[] associationId = associationId( key );
		List<byte[]> lrange = connection.lrange( associationId, 0, -1 );

		Association association = new Association();

		for ( byte[] bytes : lrange ) {
			association.getRows().add( serializationStrategy.deserialize( bytes, Object.class ) );
		}
		return association;
	}

	public Entity getEntity(EntityKey key, AssociationContext associationTypeContext) {
		return getEntityStorageStrategy(
				associationTypeContext.getAssociationTypeContext()
						.getOptionsContext()
		).getEntity( entityId( key ) );
	}

	public Entity storeEntity(EntityKey key, Entity entity, AssociationContext associationContext) {
		getEntityStorageStrategy( associationContext.getAssociationTypeContext().getOptionsContext() ).storeEntity(
				entityId( key ),
				entity
		);
		return entity;
	}

	private void expireEntity(EntityKey key, Long ttl) {
		byte[] associationId = entityId( key );
		connection.pexpire( associationId, ttl );
	}

	public void storeAssociation(EntityKey key, Association document) {

		byte[] associationId = associationId( key );
		connection.del( associationId );

		for ( Object row : document.getRows() ) {
			connection.rpush( associationId, serializationStrategy.serialize( row ) );
		}
	}

	private void expireAssociation(EntityKey key, Long ttl) {
		byte[] associationId = associationId( key );
		connection.pexpire( associationId, ttl );
	}

	public void removeAssociation(EntityKey key) {
		connection.del( associationId( key ) );
	}

	public void remove(EntityKey key) {
		connection.del( entityId( key ) );
	}


	public byte[] identifierId(IdSourceKey key) {
		byte[] prefix = toBytes( IDENTIFIERS + ":" + key.getTable() + ":" );
		byte[] entityId = prepareKey( key.getColumnNames(), key.getColumnValues() );

		byte[] identifierId = new byte[prefix.length + entityId.length];
		System.arraycopy( prefix, 0, identifierId, 0, prefix.length );
		System.arraycopy( entityId, 0, identifierId, prefix.length, entityId.length );

		return identifierId;

	}

	public byte[] associationId(EntityKey key) {
		byte[] prefix = toBytes( ASSOCIATIONS + ":" + key.getTable() + ":" );
		byte[] entityId = prepareKey( key.getColumnNames(), key.getColumnValues() );

		byte[] associationId = new byte[prefix.length + entityId.length];
		System.arraycopy( prefix, 0, associationId, 0, prefix.length );
		System.arraycopy( entityId, 0, associationId, prefix.length, entityId.length );

		return associationId;

	}

	public byte[] entityId(EntityKey key) {
		byte[] prefix = toBytes( key.getTable() + ":" );
		byte[] entityId = prepareKey( key.getColumnNames(), key.getColumnValues() );

		byte[] associationId = new byte[prefix.length + entityId.length];
		System.arraycopy( prefix, 0, associationId, 0, prefix.length );
		System.arraycopy( entityId, 0, associationId, prefix.length, entityId.length );

		return associationId;

	}

	private byte[] prepareKey(String[] columnNames, Object[] columnValues) {
		if ( columnNames.length == 1 ) {

			if ( columnValues[0] instanceof CharSequence ) {
				return columnValues[0].toString().getBytes();
			}

			return serializationStrategy.serialize( columnValues[0] );
		}

		Collator collator = Collator.getInstance( Locale.ENGLISH );
		collator.setStrength( Collator.SECONDARY );

		Map<String, Object> idObject = new TreeMap<>( collator );

		for ( int i = 0; i < columnNames.length; i++ ) {
			idObject.put( columnNames[i], columnValues[i] );
		}

		return serializationStrategy.serialize( idObject );
	}


	/**
	 * Convert a String to a byte array with UTF-8 encoding.
	 *
	 * @param string the String.
	 *
	 * @return byte array. Byte array is empty if the {@code string} is null.
	 */
	public static byte[] toBytes(String string) {
		if ( string == null ) {
			return new byte[0];
		}

		return string.getBytes( LettuceCharsets.UTF8 );
	}

	/**
	 * Convert bytes to String expecting UTF-8 encoding.
	 *
	 * @param bytes the bytes
	 *
	 * @return the String or null
	 */
	public static String toString(byte[] bytes) {
		if ( bytes == null ) {
			return null;
		}
		return new String( bytes, 0, bytes.length, LettuceCharsets.UTF8 );
	}

}
