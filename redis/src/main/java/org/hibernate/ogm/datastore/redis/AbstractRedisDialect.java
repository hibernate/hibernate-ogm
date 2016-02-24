/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisTupleSnapshot;
import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.datastore.redis.impl.json.JsonSerializationStrategy;
import org.hibernate.ogm.datastore.redis.logging.impl.Log;
import org.hibernate.ogm.datastore.redis.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.redis.options.impl.TTLOption;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationType;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsContext;

import com.lambdaworks.redis.KeyScanCursor;
import com.lambdaworks.redis.ScanArgs;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

import static org.hibernate.ogm.datastore.document.impl.DotPatternMapHelpers.getColumnSharedPrefixOfAssociatedEntityLink;

/**
 * @author Mark Paluch
 */
public abstract class AbstractRedisDialect extends BaseGridDialect {

	public static final String IDENTIFIERS = "Identifiers";
	public static final String ASSOCIATIONS = "Associations";

	private static final Pattern MODE_PATTERN = Pattern.compile( "^redis_mode:([a-z]+)$", Pattern.MULTILINE );
	private static final Log log = LoggerFactory.getLogger();

	protected final RedisClusterCommands<String, String> connection;
	protected final JsonSerializationStrategy strategy = new JsonSerializationStrategy();
	protected final boolean clusterMode;

	/**
	 * Creates a new {@link AbstractRedisDialect}.
	 *
	 * @param connection a Redis connection (A regular Redis connection implements also {@link RedisClusterCommands})
	 */
	public AbstractRedisDialect(RedisClusterCommands<String, String> connection) {

		this.connection = connection;

		String redisMode = getRedisMode( connection );
		if ( redisMode != null ) {
			log.connectedRedisNodeRunsIn( redisMode );
			clusterMode = "cluster".equalsIgnoreCase( redisMode.trim() );
		}
		else {
			clusterMode = false;
		}
	}

	private String getRedisMode(RedisClusterCommands<String, String> connection) {
		String info = connection.info( "server" );
		Matcher matcher = MODE_PATTERN.matcher( info );
		if ( matcher.find() ) {
			return matcher.group( 1 );
		}

		log.cannotDetermineRedisMode( info );
		return null;
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		return new Tuple( new RedisTupleSnapshot( new HashMap<String, Object>() ) );
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		String key = identifierId( request.getKey() );
		String value = connection.get( key );

		if ( value == null ) {
			connection.set( key, Long.toString( request.getInitialValue() ) );
			return request.getInitialValue();
		}

		return connection.incrby( key, request.getIncrement() );
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	/**
	 * Create a String representation of the identifier key in the format of {@code Identifiers:(table name):(columnId)}.
	 * {@see #IDENTIFIERS}
	 *
	 * @param key Key for the identifier
	 *
	 * @return byte array containing the key
	 */
	protected String identifierId(IdSourceKey key) {
		String prefix = IDENTIFIERS + ":" + key.getTable();

		if ( key.getColumnNames() != null ) {
			String entityId = keyToString( key.getColumnNames(), key.getColumnValues() );
			return prefix + ":" + entityId;
		}
		return prefix;
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		remove( key );
	}

	protected void addKeyValuesFromKeyName(
			EntityKeyMetadata entityKeyMetadata,
			String prefix,
			String key,
			Entity document) {
		if ( key.startsWith( prefix ) ) {

			String keyWithoutPrefix = getKeyWithoutTablePrefix( prefix, key );

			Map<String, Object> keys = keyStringToMap( entityKeyMetadata, keyWithoutPrefix );

			for ( Map.Entry<String, Object> entry : keys.entrySet() ) {
				document.set( entry.getKey(), entry.getValue() );
			}
		}
	}

	protected static Object getAssociationRow(Tuple row, AssociationKey associationKey) {
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


	protected Long getTTL(OptionsContext optionsContext) {
		return optionsContext.getUnique( TTLOption.class );
	}

	protected Long getTTL(AssociationContext associationContext) {
		return associationContext.getAssociationTypeContext().getOptionsContext().getUnique( TTLOption.class );
	}


	protected String getKeyWithoutTablePrefix(String prefixBytes, String key) {
		return key.substring( prefixBytes.length() );
	}

	protected void setAssociationTTL(
			AssociationKey associationKey,
			AssociationContext associationContext,
			Long currentTtl) {
		Long ttl = getTTL( associationContext );
		if ( ttl != null ) {
			expireAssociation( associationKey, ttl );
		}
		else if ( currentTtl != null && currentTtl > 0 ) {
			expireAssociation( associationKey, currentTtl );
		}
	}

	protected void setEntityTTL(EntityKey key, Long currentTtl, Long configuredTTL) {
		if ( configuredTTL != null ) {
			expireEntity( key, configuredTTL );
		}
		else if ( currentTtl != null && currentTtl > 0 ) {
			expireEntity( key, currentTtl );
		}
	}

	protected void expireEntity(EntityKey key, Long ttl) {
		String associationId = entityId( key );
		connection.pexpire( associationId, ttl );
	}


	protected void expireAssociation(AssociationKey key, Long ttl) {
		String associationId = associationId( key );
		connection.pexpire( associationId, ttl );
	}

	protected void removeAssociation(AssociationKey key) {
		connection.del( associationId( key ) );
	}

	protected void remove(EntityKey key) {
		connection.del( entityId( key ) );
	}

	/**
	 * Deconstruct the key name into its components:
	 * Single key: Use the value from the key
	 * Multiple keys: De-serialize the JSON map.
	 */
	protected Map<String, Object> keyStringToMap(EntityKeyMetadata entityKeyMetadata, String key) {
		if ( entityKeyMetadata.getColumnNames().length == 1 ) {
			return Collections.singletonMap( entityKeyMetadata.getColumnNames()[0], (Object) key );
		}
		return strategy.deserialize( key, Map.class );
	}

	protected void addIdToEntity(Entity entity, String[] columnNames, Object[] columnValues) {
		for ( int i = 0; i < columnNames.length; i++ ) {
			entity.set( columnNames[i], columnValues[i] );
		}
	}

	/**
	 * Create a String representation of the entity key in the format of {@code Association:(table name):(columnId)}.
	 * {@see #ASSOCIATIONS}
	 *
	 * @param key Key of the association
	 *
	 * @return byte array containing the key
	 */
	protected String associationId(AssociationKey key) {
		String prefix = ASSOCIATIONS + ":" + key.getTable() + ":";
		String entityId = keyToString( key.getColumnNames(), key.getColumnValues() ) + ":" + key.getMetadata()
				.getCollectionRole();

		return prefix + entityId;
	}

	/**
	 * Create a String representation of the key in the format of {@code (table name):(columnId)}.
	 *
	 * @param key Key of the entity
	 *
	 * @return byte array containing the key
	 */
	public String entityId(EntityKey key) {
		String prefix = key.getTable() + ":";
		String entityId = keyToString( key.getColumnNames(), key.getColumnValues() );

		return prefix + entityId;
	}

	/**
	 * Construct a key based on the key columns:
	 * Single key: Use the value as key
	 * Multiple keys: Serialize the key using a JSON map.
	 */
	private String keyToString(String[] columnNames, Object[] columnValues) {
		if ( columnNames.length == 1 ) {
			return columnValues[0].toString();
		}

		Collator collator = Collator.getInstance( Locale.ENGLISH );
		collator.setStrength( Collator.SECONDARY );

		Map<String, Object> idObject = new TreeMap<>( collator );

		for ( int i = 0; i < columnNames.length; i++ ) {
			idObject.put( columnNames[i], columnValues[i] );
		}

		return strategy.serialize( idObject );
	}

	/**
	 * Deconstruct the key name into its components:
	 * Single key: Use the value from the key
	 * Multiple keys: De-serialize the JSON map.
	 */
	protected Map<String, Object> keyToMap(EntityKeyMetadata entityKeyMetadata, String key) {
		if ( entityKeyMetadata.getColumnNames().length == 1 ) {
			return Collections.singletonMap( entityKeyMetadata.getColumnNames()[0], (Object) key );
		}
		return strategy.deserialize( key, Map.class );
	}

	/**
	 * Retrieve association from a Redis List or Redis Set, depending on the association type.
	 *
	 * @param key the association key
	 *
	 * @return the association
	 */
	protected org.hibernate.ogm.datastore.redis.dialect.value.Association getAssociation(AssociationKey key) {
		String associationId = associationId( key );
		Collection<String> rows;

		if ( key.getMetadata().getAssociationType() == AssociationType.SET ) {
			rows = connection.smembers( associationId );
		}
		else {
			rows = connection.lrange( associationId, 0, -1 );
		}

		org.hibernate.ogm.datastore.redis.dialect.value.Association association = new org.hibernate.ogm.datastore.redis.dialect.value.Association();

		for ( String item : rows ) {
			association.getRows().add( strategy.deserialize( item, Object.class ) );
		}
		return association;
	}

	/**
	 * Store an association to a Redis List or Redis Set, depending on the association type.
	 *
	 * @param key the association key
	 * @param association the association document
	 */
	protected void storeAssociation(
			AssociationKey key,
			org.hibernate.ogm.datastore.redis.dialect.value.Association association) {
		String associationId = associationId( key );
		connection.del( associationId );

		for ( Object row : association.getRows() ) {
			if ( key.getMetadata().getAssociationType() == AssociationType.SET ) {
				connection.sadd( associationId, strategy.serialize( row ) );
			}
			else {
				connection.rpush( associationId, strategy.serialize( row ) );
			}
		}
	}

	/**
	 * Scan over keys. This method is aware whether the the client is connected to a Redis Cluster.
	 * If so, then a Redis Cluster scan requires to iterate over master nodes and keep the
	 * state within a custom {@link KeyScanCursor} instance.
	 *
	 * @param cursor
	 * @param scanArgs
	 *
	 * @return
	 */
	protected KeyScanCursor<String> scan(KeyScanCursor<String> cursor, ScanArgs scanArgs) {

		if ( !clusterMode ) {
			return scan( connection, cursor, scanArgs );
		}

		return clusterScan( cursor, scanArgs );
	}

	private KeyScanCursor<String> scan(
			RedisClusterCommands<String, String> commands,
			KeyScanCursor<String> cursor,
			ScanArgs scanArgs) {
		if ( cursor != null ) {
			return commands.scan( cursor, scanArgs );
		}
		return commands.scan( scanArgs );
	}

	@SuppressWarnings("unchecked")
	private KeyScanCursor<String> clusterScan(KeyScanCursor<String> cursor, ScanArgs scanArgs) {

		RedisAdvancedClusterCommands<String, String> commands = (RedisAdvancedClusterCommands<String, String>) connection;
		List<String> nodeIds;
		String currentNodeId;

		if ( cursor == null ) {
			Set<RedisClusterNode> masterNodes = commands.masters().asMap().keySet();
			nodeIds = new ArrayList<>();

			for ( RedisClusterNode masterNode : masterNodes ) {
				nodeIds.add( masterNode.getNodeId() );
			}

			if ( nodeIds.isEmpty() ) {
				return scan( connection, cursor, scanArgs );
			}

			currentNodeId = nodeIds.get( 0 );
		}
		else {
			if ( !( cursor instanceof ClusterwideKeyScanCursor ) ) {
				throw new IllegalArgumentException(
						"A scan in Redis Cluster mode requires to reuse the resulting cursor from the previous scan invocation" );
			}

			ClusterwideKeyScanCursor<String> clusterKeyScanCursor = (ClusterwideKeyScanCursor<String>) cursor;
			nodeIds = clusterKeyScanCursor.nodeIds;

			currentNodeId = getNodeIdForNextScanIteration( nodeIds, clusterKeyScanCursor );
		}

		RedisClusterCommands<String, String> nodeConnection = commands.getConnection( currentNodeId );
		KeyScanCursor<String> nodeKeyScanCursor = scan( nodeConnection, cursor, scanArgs );

		return new ClusterwideKeyScanCursor(
				nodeIds,
				currentNodeId,
				nodeKeyScanCursor
		);
	}

	private String getNodeIdForNextScanIteration(
			List<String> nodeIds,
			ClusterwideKeyScanCursor<String> clusterKeyScanCursor) {
		if ( clusterKeyScanCursor.isScanOnCurrentNodeFinished() ) {
			if ( clusterKeyScanCursor.isFinished() ) {
				throw new IllegalStateException( "Cluster scan is finished" );
			}

			int nodeIndex = nodeIds.indexOf( clusterKeyScanCursor.currentNodeId );
			return nodeIds.get( nodeIndex + 1 );
		}

		return clusterKeyScanCursor.currentNodeId;
	}

	/**
	 * @return {@literal true} if the connected Redis node (default connection) is running in Redis Cluster mode.
	 */
	public boolean isClusterMode() {
		return clusterMode;
	}

	static class ClusterwideKeyScanCursor<K> extends KeyScanCursor<K> {
		final List<String> nodeIds;
		final String currentNodeId;
		final KeyScanCursor<K> cursor;

		public ClusterwideKeyScanCursor(List<String> nodeIds, String currentNodeId, KeyScanCursor<K> cursor) {
			super();
			this.nodeIds = nodeIds;
			this.currentNodeId = currentNodeId;
			this.cursor = cursor;
			setCursor( cursor.getCursor() );
			getKeys().addAll( cursor.getKeys() );

			if ( cursor.isFinished() ) {
				int nodeIndex = nodeIds.indexOf( currentNodeId );
				if ( nodeIndex == -1 || nodeIndex == nodeIds.size() - 1 ) {
					setFinished( true );
				}
			}

		}

		private boolean isScanOnCurrentNodeFinished() {
			return cursor.isFinished();
		}

	}
}
