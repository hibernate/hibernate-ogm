/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.spi.AssociationStorageOption;
import org.hibernate.ogm.datastore.map.impl.MapHelpers;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisAssociation;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisAssociationSnapshot;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisTupleSnapshot;
import org.hibernate.ogm.datastore.redis.dialect.value.Association;
import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.redis.impl.SerializationStrategy;
import org.hibernate.ogm.datastore.redis.impl.json.JsonSerializationStrategy;
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
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.AssociationKind;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.Type;

import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.output.KeyValueStreamingChannel;
import com.lambdaworks.redis.protocol.LettuceCharsets;

/**
 * Stores tuples and associations inside Redis.
 * <p/>
 * Tuples are stored in Redis as a JSON serialization of a {@link Entity} object. Associations are stored in Redis obtained as a
 * JSON serialization of a {@link Association} object.
 *
 * @author Mark Paluch
 */
public class RedisDialect extends BaseGridDialect {

	public final static byte[] IDENTIFIERS = toBytes( "Identifiers" );
	public final static byte[] ASSOCIATIONS = toBytes( "Associations" );

	private final RedisDatastoreProvider provider;
	private final RedisConnection<byte[], byte[]> connection;

	// TODO: JavaSerialization
	private final SerializationStrategy serializationStrategy = new JsonSerializationStrategy();

    /*
	 * TODO: In general: choosing between two general storing strategies. Redis is a Key-Value store. Currently, the only really
     * interesting store is Hashes. A hash is a map identified by another id, similar to Map<Object, Map<Object, Object>>.
     * 
     * The current strategy uses table as Hash-Id and the Entity-Id as key within the hash. Records are projected as
     * semi-documents.
     * 
     * Another way to store entities would be storing the key-values pairs with Hash-Id=Table+EntityId (no composite id) and
     * storing the map directly as key-values. Redis knows only integer any byte[] data types so data type support would be
     * limited to String, int, long, other numeric types as string, byte[] and Dates-as-String.
     */

	public RedisDialect(RedisDatastoreProvider provider) {
		this.provider = provider;
		this.connection = provider.getConnection();
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

		Entity entity = getEntity( key );
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
		storeEntity( key, map );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		remove( key );
	}

	@Override
	public boolean isStoredInEntityStructure(
			AssociationKeyMetadata associationKeyMetadata,
			AssociationTypeContext associationTypeContext) {
		AssociationStorageType associationStorage = associationTypeContext.getOptionsContext().getUnique(
				AssociationStorageOption.class
		);

		return associationKeyMetadata.isOneToOne()
				|| associationKeyMetadata.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION
				|| associationStorage == AssociationStorageType.IN_ENTITY;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		byte[] key = prepareKey( request.getKey().getColumnNames(), request.getKey().getColumnValues() );
		byte[] hget = connection.hget( IDENTIFIERS, key );

		if ( hget == null || hget.length == 0 ) {
			connection.hset( IDENTIFIERS, key, toBytes( Long.toString( request.getInitialValue() ) ) );
			return request.getInitialValue();
		}

		return connection.hincrby( IDENTIFIERS, key, request.getIncrement() );

	}

	@Override
	public void forEachTuple(final ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {

		// ModelConsumer should not do blocking things.
		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
			connection.hgetall(
					new KeyValueStreamingChannel<byte[], byte[]>() {
						@Override
						public void onKeyValue(byte[] key, byte[] value) {
							Entity document = serializationStrategy.deserialize( value, Entity.class );
							consumer.consume( new Tuple( new RedisTupleSnapshot( document.getProperties() ) ) );
						}
					}, toBytes( entityKeyMetadata.getTable() )
			);
		}
	}

	public static byte[] toBytes(String string) {
		return string.getBytes( LettuceCharsets.UTF8 );
	}

	public static String toString(byte[] bytes) {
		if ( bytes == null ) {
			return null;
		}
		return new String( bytes, 0, bytes.length, LettuceCharsets.UTF8 );
	}

	@Override
	public org.hibernate.ogm.model.spi.Association getAssociation(
			AssociationKey key,
			AssociationContext associationContext) {
		RedisAssociation redisAssociation = null;

		if ( isStoredInEntityStructure( key.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
			Entity owningEntity = getEntity( key.getEntityKey() );
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
			Entity owningEntity = getEntity( key.getEntityKey() );
			if ( owningEntity == null ) {
				owningEntity = storeEntity( key.getEntityKey(), new Entity() );
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
			storeEntity( associationKey.getEntityKey(), (Entity) redisAssociation.getOwningDocument() );
		}
		else {
			storeAssociation( associationKey.getEntityKey(), (Association) redisAssociation.getOwningDocument() );
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
			Entity owningEntity = getEntity( key.getEntityKey() );
			if ( owningEntity != null ) {
				owningEntity.removeAssociation( key.getMetadata().getCollectionRole() );
				storeEntity( key.getEntityKey(), owningEntity );
			}
		}
		else {
			removeAssociation( key.getEntityKey() );
		}
	}

	public Entity getEntity(EntityKey key) {

		byte[] value = connection.hget(
				toBytes( key.getTable() ), prepareKey(
						key.getColumnNames(),
						key.getColumnValues()
				)
		);

		return serializationStrategy.deserialize( value, Entity.class );
	}

	public Association getAssociation(EntityKey key) {

		String associationId = key.getTable() + ":" + prepareKey( key.getColumnNames(), key.getColumnValues() );
		byte[] value = connection.hget( ASSOCIATIONS, toBytes( associationId ) );

		return serializationStrategy.deserialize( value, Association.class );
	}

	public Association storeAssociation(EntityKey key, Association document) {

		String associationId = key.getTable() + ":" + prepareKey( key.getColumnNames(), key.getColumnValues() );
		byte[] value = serializationStrategy.serialize( document );
		connection.hset( ASSOCIATIONS, toBytes( associationId ), value );
		return document;
	}

	public void removeAssociation(EntityKey key) {

		String associationId = key.getTable() + ":" + prepareKey( key.getColumnNames(), key.getColumnValues() );
		connection.hdel( ASSOCIATIONS, toBytes( associationId ) );
	}

	public void storeEntity(EntityKey key, Map<String, Object> map) {
		Entity entityDocument = new Entity();

		for ( Map.Entry<String, Object> entry : map.entrySet() ) {
			entityDocument.set( entry.getKey(), entry.getValue() );
		}

		storeEntity( key, entityDocument );
	}

	public Entity storeEntity(EntityKey key, Entity document) {

		byte value[] = serializationStrategy.serialize( document );
		connection.hset( toBytes( key.getTable() ), prepareKey( key.getColumnNames(), key.getColumnValues() ), value );
		return document;
	}

	public void remove(EntityKey key) {
		connection.hdel( toBytes( key.getTable() ), prepareKey( key.getColumnNames(), key.getColumnValues() ) );
	}

	private byte[] prepareKey(String[] columnNames, Object[] columnValues) {
		if ( columnNames.length == 1 ) {

			if ( columnValues[0] instanceof CharSequence ) {
				return columnValues[0].toString().getBytes();
			}

			return serializationStrategy.serialize( columnValues[0] );
		}

		Map<String, Object> idObject = new TreeMap<>();

		for ( int i = 0; i < columnNames.length; i++ ) {
			idObject.put( columnNames[i], columnValues[i] );
		}

		return serializationStrategy.serialize( idObject );
	}

}
