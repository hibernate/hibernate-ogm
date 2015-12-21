/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis;

import java.text.Collator;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisTupleSnapshot;
import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.datastore.redis.impl.json.JsonSerializationStrategy;
import org.hibernate.ogm.datastore.redis.options.impl.TTLOption;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.options.spi.OptionsContext;

import com.lambdaworks.redis.RedisConnection;

import static org.hibernate.ogm.datastore.document.impl.DotPatternMapHelpers.getColumnSharedPrefixOfAssociatedEntityLink;

/**
 * @author Mark Paluch
 */
public abstract class AbstractRedisDialect extends BaseGridDialect {

	public static final String IDENTIFIERS = "Identifiers";
	public static final String ASSOCIATIONS = "Associations";

	protected final RedisConnection<String, String> connection;
	protected final JsonSerializationStrategy strategy = new JsonSerializationStrategy();


	public AbstractRedisDialect(RedisConnection<String, String> connection) {

		this.connection = connection;
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		return new Tuple( new RedisTupleSnapshot( new HashMap<String, Object>() ) );
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		String key = identifierId( request.getKey() );
		String hget = connection.get( key );

		if ( hget == null ) {
			connection.set( key, Long.toString( request.getInitialValue() ) );
			return request.getInitialValue();
		}

		return connection.incrby( key, request.getIncrement() );
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
		String prefix = IDENTIFIERS + ":" + key.getTable() + ":";
		String entityId = keyToString( key.getColumnNames(), key.getColumnValues() );

		return prefix + entityId;
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
		if ( startsWith( key, prefix ) ) {

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

	/**
	 * Check, whether {@code bytes} starts with {@code prefixBytes}.
	 *
	 * @param bytes haystack
	 * @param prefixBytes needle
	 *
	 * @return true, if {@code bytes} starts with {@code prefixBytes}
	 */
	protected boolean startsWith(String bytes, String prefixBytes) {
		return bytes.startsWith( prefixBytes );

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
		String prefix = ASSOCIATIONS + ":" + key.getMetadata().getTable() + ":" + key.getMetadata()
				.getCollectionRole() + ":";
		String entityId = keyToString( key.getColumnNames(), key.getColumnValues() );

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

}
