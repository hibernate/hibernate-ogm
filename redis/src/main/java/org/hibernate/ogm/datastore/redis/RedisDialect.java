/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.ogm.datastore.document.association.spi.impl.DocumentHelpers;
import org.hibernate.ogm.datastore.document.impl.DotPatternMapHelpers;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.spi.AssociationStorageOption;
import org.hibernate.ogm.datastore.map.impl.MapHelpers;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisAssociation;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisAssociationSnapshot;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisTupleSnapshot;
import org.hibernate.ogm.datastore.redis.dialect.value.Association;
import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.redis.impl.json.JsonEntityStorageStrategy;
import org.hibernate.ogm.datastore.redis.impl.json.JsonSerializationStrategy;
import org.hibernate.ogm.datastore.redis.options.impl.TTLOption;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
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
import org.hibernate.ogm.model.spi.TupleOperation;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.Type;

import com.lambdaworks.redis.KeyScanCursor;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.ScanArgs;
import com.lambdaworks.redis.protocol.LettuceCharsets;

/**
 * Stores tuples and associations inside Redis.
 * <p>
 * Tuples are stored in Redis as a JSON serialization of a {@link Entity} object. Associations are stored in Redis obtained as a
 * JSON serialization of a {@link Association} object either within the entity or external.
 * See {@link org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties#ASSOCIATIONS_STORE} on how to configure
 * entity or external storage.
 *
 * @author Mark Paluch
 */
public class RedisDialect extends BaseGridDialect implements MultigetGridDialect {

	public static final String IDENTIFIERS = "Identifiers";
	public static final String ASSOCIATIONS = "Associations";

	protected final JsonEntityStorageStrategy entityStorageStrategy;
	private final RedisConnection<byte[], byte[]> connection;
	private final JsonSerializationStrategy serializationStrategy = new JsonSerializationStrategy();

	public RedisDialect(RedisDatastoreProvider provider) {
		this.connection = provider.getConnection();
		this.entityStorageStrategy = new JsonEntityStorageStrategy( serializationStrategy, connection );
	}

	@Override
	public GridType overrideType(Type type) {
		return serializationStrategy.overrideType( type );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		Entity entity = entityStorageStrategy.getEntity( entityId( key ) );

		if ( entity != null ) {
			return new Tuple( new RedisTupleSnapshot( entity.getProperties() ) );
		}
		else {
			return null;
		}
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		return new Tuple( new RedisTupleSnapshot( new HashMap<String, Object>() ) );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		Map<String, Object> map = ( (RedisTupleSnapshot) tuple.getSnapshot() ).getMap();
		MapHelpers.applyTupleOpsOnMap( tuple, map );
		storeEntity( key, map, tupleContext.getOptionsContext(), tuple.getOperations() );
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
			String prefix = entityKeyMetadata.getTable() + ":";
			byte[] prefixBytes = toBytes( prefix );

			ScanArgs scanArgs = ScanArgs.Builder.matches( prefix + "*" );
			do {
				if ( cursor != null ) {
					cursor = connection.scan( cursor, scanArgs );
				}
				else {
					cursor = connection.scan( scanArgs );
				}

				for ( byte[] key : cursor.getKeys() ) {
					Entity document = entityStorageStrategy.getEntity( key );

					addKeyValuesFromKeyName( entityKeyMetadata, prefixBytes, key, document );

					consumer.consume( new Tuple( new RedisTupleSnapshot( document.getProperties() ) ) );
				}

			} while ( !cursor.isFinished() );
		}
	}

	private void addKeyValuesFromKeyName(
			EntityKeyMetadata entityKeyMetadata,
			byte[] prefix,
			byte[] key,
			Entity document) {
		if ( startsWith( key, prefix ) ) {

			byte[] keyWithoutPrefix = getKeyWithoutTablePrefix( prefix, key );

			Map<String, Object> keys = keyBytesToMap( entityKeyMetadata, keyWithoutPrefix );

			for ( Map.Entry<String, Object> entry : keys.entrySet() ) {
				document.set( entry.getKey(), entry.getValue() );
			}
		}
	}

	private byte[] getKeyWithoutTablePrefix(byte[] prefixBytes, byte[] key) {
		byte[] keyWithoutPrefix = new byte[key.length - prefixBytes.length];
		System.arraycopy( key, prefixBytes.length, keyWithoutPrefix, 0, keyWithoutPrefix.length );
		return keyWithoutPrefix;
	}

	/**
	 * Check, whether {@code bytes} starts with {@code prefixBytes}.
	 *
	 * @param bytes haystack
	 * @param prefixBytes needle
	 *
	 * @return true, if {@code bytes} starts with {@code prefixBytes}
	 */
	private boolean startsWith(byte[] bytes, byte[] prefixBytes) {
		if ( prefixBytes.length > bytes.length ) {
			return false;
		}

		for ( int i = 0; i < prefixBytes.length; i++ ) {
			if ( bytes[i] != prefixBytes[i] ) {
				return false;
			}
		}

		return true;

	}

	@Override
	public org.hibernate.ogm.model.spi.Association getAssociation(
			AssociationKey key,
			AssociationContext associationContext) {
		RedisAssociation redisAssociation = null;

		if ( isStoredInEntityStructure( key.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
			Entity owningEntity = getEmbeddingEntity( key );

			if ( owningEntity != null && DotPatternMapHelpers.hasField(
					owningEntity.getPropertiesAsHierarchy(),
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
		RedisAssociation redisAssociation;

		if ( isStoredInEntityStructure( key.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
			Entity owningEntity = getEmbeddingEntity( key );

			if ( owningEntity == null ) {
				owningEntity = storeEntity( key.getEntityKey(), new Entity(), associationContext );
			}

			redisAssociation = RedisAssociation.fromEmbeddedAssociation( owningEntity, key.getMetadata() );
		}
		else {
			Association association = new Association();
			redisAssociation = RedisAssociation.fromAssociationDocument( association );
		}

		return new org.hibernate.ogm.model.spi.Association(
				new RedisAssociationSnapshot(
						redisAssociation,
						key
				)
		);
	}

	// Retrieve entity that contains the association, do not enhance with entity key
	private Entity getEmbeddingEntity(AssociationKey key) {
		return entityStorageStrategy.getEntity( entityId( key.getEntityKey() ) );
	}

	@Override
	public void insertOrUpdateAssociation(
			AssociationKey associationKey, org.hibernate.ogm.model.spi.Association association,
			AssociationContext associationContext) {
		Object rows = getAssociationRows( association, associationKey, associationContext );

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
			Long currentTtl = connection.pttl( entityId( associationKey.getEntityKey() ) );
			storeAssociation( associationKey.getEntityKey(), (Association) redisAssociation.getOwningDocument() );
			setAssociationTTL( associationKey, associationContext, currentTtl );
		}
	}

	private void setAssociationTTL(
			AssociationKey associationKey,
			AssociationContext associationContext,
			Long currentTtl) {
		Long ttl = getTTL( associationContext );
		if ( ttl != null ) {
			expireAssociation( associationKey.getEntityKey(), ttl );
		}
		else if ( currentTtl != null && currentTtl > 0 ) {
			expireAssociation( associationKey.getEntityKey(), currentTtl );
		}
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
	private Object getAssociationRows(
			org.hibernate.ogm.model.spi.Association association,
			AssociationKey key,
			AssociationContext associationContext) {


		boolean organizeByRowKey = DotPatternMapHelpers.organizeAssociationMapByRowKey( association, key, associationContext );

		// only in-entity maps can be mapped by row key to prevent huge external association maps
		if ( isStoredInEntityStructure(
				key.getMetadata(),
				associationContext.getAssociationTypeContext()
		) && organizeByRowKey ) {
			String rowKeyColumn = organizeByRowKey ? key.getMetadata().getRowKeyIndexColumnNames()[0] : null;
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

		List<Object> rows = new ArrayList<Object>( association.size() );
		for ( RowKey rowKey : association.getKeys() ) {
			rows.add( getAssociationRow( association.get( rowKey ), key ) );
		}

		return rows;
	}

	private Object getAssociationRow(Tuple row, AssociationKey associationKey) {
		String[] columnsToPersist = associationKey.getMetadata()
				.getColumnsWithoutKeyColumns( row.getColumnNames() );

		// return value itself if there is only a single column to store
		if ( columnsToPersist.length == 1 ) {
			return row.get( columnsToPersist[0] );
		}
		Entity rowObject = new Entity();
		String prefix = getColumnSharedPrefixOfAssociatedEntityLink( associationKey );
		for ( String column : columnsToPersist ) {
			Object value = row.get( column );
			if ( value != null ) {
				String columnName = column.startsWith( prefix ) ? column.substring( prefix.length() ) : column;
				rowObject.set( columnName, value );
			}
		}

		return rowObject.getPropertiesAsHierarchy();
	}

	private String getColumnSharedPrefixOfAssociatedEntityLink(AssociationKey associationKey) {
		String[] associationKeyColumns = associationKey.getMetadata()
				.getAssociatedEntityKeyMetadata()
				.getAssociationKeyColumns();
		// we used to check that columns are the same (in an ordered fashion)
		// but to handle List and Map and store indexes / keys at the same level as the id columns
		// this check is removed
		String prefix = DocumentHelpers.getColumnSharedPrefix( associationKeyColumns );
		return prefix == null ? "" : prefix + ".";
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		if ( isStoredInEntityStructure( key.getMetadata(), associationContext.getAssociationTypeContext() ) ) {
			Entity owningEntity = getEmbeddingEntity( key );

			if ( owningEntity != null ) {
				owningEntity.removeAssociation( key.getMetadata().getCollectionRole() );
				storeEntity( key.getEntityKey(), owningEntity, associationContext );
			}
		}
		else {
			removeAssociation( key.getEntityKey() );
		}
	}

	private void addIdToEntity(Entity entity, String[] columnNames, Object[] columnValues) {
		for ( int i = 0; i < columnNames.length; i++ ) {
			entity.set( columnNames[i], columnValues[i] );
		}
	}

	private void storeEntity(
			EntityKey key,
			Map<String, Object> map,
			OptionsContext optionsContext,
			Set<TupleOperation> operations) {
		Entity entityDocument = new Entity();

		for ( Map.Entry<String, Object> entry : map.entrySet() ) {
			if ( key.getMetadata().isKeyColumn( entry.getKey() ) ) {
				continue;
			}
			entityDocument.set( entry.getKey(), entry.getValue() );
		}

		storeEntity( key, entityDocument, optionsContext, operations );
	}

	private void storeEntity(
			EntityKey key,
			Entity document,
			OptionsContext optionsContext,
			Set<TupleOperation> operations) {

		Long currentTtl = connection.pttl( entityId( key ) );

		entityStorageStrategy.storeEntity( entityId( key ), document, operations );

		setEntityTTL( key, currentTtl, getTTL( optionsContext ) );
	}

	private void setEntityTTL(EntityKey key, Long currentTtl, Long configuredTTL) {
		if ( configuredTTL != null ) {
			expireEntity( key, configuredTTL );
		}
		else if ( currentTtl != null && currentTtl > 0 ) {
			expireEntity( key, currentTtl );
		}
	}

	private Association getAssociation(EntityKey key) {
		byte[] associationId = associationId( key );
		List<byte[]> lrange = connection.lrange( associationId, 0, -1 );

		Association association = new Association();

		for ( byte[] bytes : lrange ) {
			association.getRows().add( serializationStrategy.deserialize( bytes, Object.class ) );
		}
		return association;
	}

	private Entity storeEntity(EntityKey key, Entity entity, AssociationContext associationContext) {
		Long currentTtl = connection.pttl( entityId( key ) );

		entityStorageStrategy.storeEntity(
				entityId( key ),
				entity,
				null
		);

		setEntityTTL( key, currentTtl, getTTL( associationContext ) );
		return entity;
	}

	private void expireEntity(EntityKey key, Long ttl) {
		byte[] associationId = entityId( key );
		connection.pexpire( associationId, ttl );
	}

	private void storeAssociation(EntityKey key, Association document) {
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

	private void removeAssociation(EntityKey key) {
		connection.del( associationId( key ) );
	}

	private void remove(EntityKey key) {
		connection.del( entityId( key ) );
	}

	/**
	 * Create a byte[] representation of the identifier key in the format of {@code Identifiers:(table name):(columnId)}.
	 * {@see #IDENTIFIERS}
	 *
	 * @param key Key for the identifier
	 *
	 * @return byte array containing the key
	 */
	private byte[] identifierId(IdSourceKey key) {
		byte[] prefix = toBytes( IDENTIFIERS + ":" + key.getTable() + ":" );
		byte[] entityId = keyToBytes( key.getColumnNames(), key.getColumnValues() );

		byte[] identifierId = new byte[prefix.length + entityId.length];
		System.arraycopy( prefix, 0, identifierId, 0, prefix.length );
		System.arraycopy( entityId, 0, identifierId, prefix.length, entityId.length );

		return identifierId;
	}

	/**
	 * Create a byte[] representation of the entity key in the format of {@code Association:(table name):(columnId)}.
	 * {@see #ASSOCIATIONS}
	 *
	 * @param key Key of the association
	 *
	 * @return byte array containing the key
	 */
	private byte[] associationId(EntityKey key) {
		byte[] prefix = toBytes( ASSOCIATIONS + ":" + key.getTable() + ":" );
		byte[] entityId = keyToBytes( key.getColumnNames(), key.getColumnValues() );

		byte[] associationId = new byte[prefix.length + entityId.length];
		System.arraycopy( prefix, 0, associationId, 0, prefix.length );
		System.arraycopy( entityId, 0, associationId, prefix.length, entityId.length );

		return associationId;
	}

	/**
	 * Create a byte[] representation of the key in the format of {@code (table name):(columnId)}.
	 *
	 * @param key Key of the entity
	 *
	 * @return byte array containing the key
	 */
	public byte[] entityId(EntityKey key) {
		byte[] prefix = toBytes( key.getTable() + ":" );
		byte[] entityId = keyToBytes( key.getColumnNames(), key.getColumnValues() );

		byte[] associationId = new byte[prefix.length + entityId.length];
		System.arraycopy( prefix, 0, associationId, 0, prefix.length );
		System.arraycopy( entityId, 0, associationId, prefix.length, entityId.length );

		return associationId;
	}

	public JsonEntityStorageStrategy getEntityStorageStrategy() {
		return entityStorageStrategy;
	}

	/**
	 * Construct a key based on the key columns:
	 * Single key: Use the value as key
	 * Multiple keys: Serialize the key using a JSON map.
	 */
	private byte[] keyToBytes(String[] columnNames, Object[] columnValues) {
		if ( columnNames.length == 1 ) {
			return toBytes( columnValues[0].toString() );
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
	 * Deconstruct the key name into its components:
	 * Single key: Use the value from the key
	 * Multiple keys: De-serialize the JSON map.
	 */
	private Map<String, Object> keyBytesToMap(EntityKeyMetadata entityKeyMetadata, byte[] key) {
		if ( entityKeyMetadata.getColumnNames().length == 1 ) {
			return Collections.singletonMap( entityKeyMetadata.getColumnNames()[0], (Object) toString( key ) );
		}
		return serializationStrategy.deserialize( key, Map.class );
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

	// MultigetGridDialect

	@Override
	public List<Tuple> getTuples(EntityKey[] keys, TupleContext tupleContext) {
		byte[][] ids = new byte[keys.length][];

		for ( int i = 0; i < keys.length; i++ ) {
			ids[i] = entityId( keys[i] );
		}

		Iterable<Entity> entities = entityStorageStrategy.getEntities( ids );
		List<Tuple> tuples = new ArrayList<Tuple>( keys.length );

		int i = 0;
		for ( Entity entity : entities ) {
			if ( entity != null ) {
				EntityKey key = keys[i];
				addIdToEntity( entity, key.getColumnNames(), key.getColumnValues() );
				tuples.add( new Tuple( new RedisTupleSnapshot( entity.getProperties() ) ) );
			}
			i++;
		}

		return tuples;
	}
}
