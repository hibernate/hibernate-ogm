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

import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisAssociation;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisAssociationSnapshot;
import org.hibernate.ogm.datastore.redis.dialect.model.impl.RedisTupleSnapshot;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.redis.impl.hash.RedisHashTypeConverter;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.TupleOperation;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.Type;

import com.lambdaworks.redis.KeyScanCursor;
import com.lambdaworks.redis.ScanArgs;

/**
* Stores tuples and associations inside Redis using hash data structures.
 * <p>
 * Tuples are stored in Redis within hashes. Associations are stored in Redis obtained as either single values or a
 * JSON serialization of a {@link org.hibernate.ogm.datastore.redis.dialect.value.Association} object in list data structures.
 *
 * @author Mark Paluch
 */
public class RedisHashDialect extends AbstractRedisDialect {

	public RedisHashDialect(RedisDatastoreProvider provider) {
		super( provider.getConnection() );
	}

	@Override
	public GridType overrideType(Type type) {
		return RedisHashTypeConverter.INSTANCE.convert( type );
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Tuple getTuple(
			EntityKey key, TupleContext tupleContext) {
		String entityIdString = entityId( key );
		if ( !connection.exists( entityIdString ) ) {
			return null;
		}

		Map<String, Object> objects;
		if ( tupleContext.getSelectableColumns().isEmpty() ) {
			objects = (Map) connection.hgetall( entityIdString );
		}
		else {
			List<String> hmget = connection.hmget( entityIdString, getFields( tupleContext ) );
			objects = toEntity( tupleContext, hmget );
		}

		return new Tuple( new RedisTupleSnapshot( objects ) );
	}

	private Map<String, Object> toEntity(TupleContext tupleContext, List<String> hmget) {
		Map<String, Object> objects = new HashMap<>();
		for ( int i = 0; i < tupleContext.getSelectableColumns().size(); i++ ) {
			String columnName = tupleContext.getSelectableColumns().get( i );
			String value = hmget.get( i );
			if ( value == null ) {
				continue;
			}
			objects.put( columnName, value );
		}
		return objects;
	}

	private String[] getFields(TupleContext tupleContext) {
		return tupleContext.getSelectableColumns().toArray( new String[tupleContext.getSelectableColumns().size()] );
	}

	@Override
	public void insertOrUpdateTuple(
			EntityKey key, Tuple tuple, TupleContext tupleContext) throws TupleAlreadyExistsException {
		Map<String, Object> map = ( (RedisTupleSnapshot) tuple.getSnapshot() ).getMap();
		Map<String, String> entity = new HashMap<>();
		List<String> toDelete1 = new ArrayList<>();

		for ( TupleOperation action : tuple.getOperations() ) {
			switch ( action.getType() ) {
				case PUT:
					map.put( action.getColumn(), action.getValue() );

					// TODO: IllegalStateException when value is not String/Character?
					if ( action.getValue() instanceof Character ) {
						entity.put( action.getColumn(), action.getValue().toString() );
					}
					else {
						entity.put( action.getColumn(), (String) action.getValue() );
					}
					break;
				case REMOVE:
				case PUT_NULL:
					map.remove( action.getColumn() );
					toDelete1.add( action.getColumn() );
					break;
			}
		}

		String entityId = entityId( key );
		Long currentTtl = connection.pttl( entityId( key ) );

		if ( !toDelete1.isEmpty() ) {
			String[] toDelete2 = toDelete1.toArray( new String[toDelete1.size()] );
			connection.hdel( entityId, toDelete2 );
		}

		if ( !entity.isEmpty() ) {
			connection.hmset( entityId, entity );
		}

		setEntityTTL( key, currentTtl, getTTL( tupleContext.getOptionsContext() ) );
	}

	@Override
	public Association getAssociation(
			AssociationKey key, AssociationContext associationContext) {
		org.hibernate.ogm.datastore.redis.dialect.value.Association association = getAssociation( key );

		if ( association == null ) {
			return null;
		}

		RedisAssociation redisAssociation = RedisAssociation.fromAssociationDocument( association );

		return redisAssociation != null ? new org.hibernate.ogm.model.spi.Association(
				new RedisAssociationSnapshot(
						redisAssociation, key
				)
		) : null;
	}

	@Override
	public Association createAssociation(
			AssociationKey key, AssociationContext associationContext) {
		org.hibernate.ogm.datastore.redis.dialect.value.Association association = getAssociation( key );

		if ( association == null ) {
			return null;
		}

		RedisAssociation redisAssociation = RedisAssociation.fromAssociationDocument( association );

		return new org.hibernate.ogm.model.spi.Association(
				new RedisAssociationSnapshot(
						redisAssociation, key
				)
		);
	}

	private org.hibernate.ogm.datastore.redis.dialect.value.Association getAssociation(AssociationKey key) {
		String associationId = associationId( key );
		List<String> lrange = connection.lrange( associationId, 0, -1 );

		org.hibernate.ogm.datastore.redis.dialect.value.Association association = new org.hibernate.ogm.datastore.redis.dialect.value.Association();

		for ( String item : lrange ) {
			association.getRows().add( strategy.deserialize( item, Object.class ) );
		}
		return association;
	}

	@Override
	public void insertOrUpdateAssociation(
			AssociationKey associationKey, Association association, AssociationContext associationContext) {
		Object rows = getAssociationRows( association, associationKey );

		RedisAssociation redisAssociation = ( (RedisAssociationSnapshot) association.getSnapshot() ).getRedisAssociation();
		redisAssociation.setRows( rows );

		Long currentTtl = connection.pttl( associationId( associationKey ) );
		storeAssociation(
				associationKey,
				(org.hibernate.ogm.datastore.redis.dialect.value.Association) redisAssociation.getOwningDocument()
		);
		setAssociationTTL( associationKey, associationContext, currentTtl );
	}

	private void storeAssociation(
			AssociationKey key,
			org.hibernate.ogm.datastore.redis.dialect.value.Association document) {
		String associationId = associationId( key );
		connection.del( associationId );

		for ( Object row : document.getRows() ) {
			connection.rpush( associationId, strategy.serialize( row ) );
		}
	}

	private Object getAssociationRows(
			Association association,
			AssociationKey key) {
		List<Object> rows = new ArrayList<Object>( association.size() );
		for ( RowKey rowKey : association.getKeys() ) {
			rows.add( getAssociationRow( association.get( rowKey ), key ) );
		}

		return rows;
	}

	@Override
	public void removeAssociation(
			AssociationKey key, AssociationContext associationContext) {
		connection.del( associationId( key ) );
	}

	@Override
	public boolean isStoredInEntityStructure(
			AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public void forEachTuple(
			ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {

		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
			KeyScanCursor<String> cursor = null;
			String prefix = entityKeyMetadata.getTable() + ":";

			ScanArgs scanArgs = ScanArgs.Builder.matches( prefix + "*" );
			do {
				if ( cursor != null ) {
					cursor = connection.scan( cursor, scanArgs );
				}
				else {
					cursor = connection.scan( scanArgs );
				}

				for ( String key : cursor.getKeys() ) {
					Map<String, String> hgetall = connection.hgetall( key );
					Map<String, Object> entity = new HashMap<>();

					entity.putAll( hgetall );
					addKeyValuesFromKeyName( entityKeyMetadata, prefix, key, entity );
					consumer.consume( new Tuple( new RedisTupleSnapshot( entity ) ) );
				}

			} while ( !cursor.isFinished() );
		}
	}

	protected void addKeyValuesFromKeyName(
			EntityKeyMetadata entityKeyMetadata,
			String prefix,
			String key,
			Map<String, Object> document) {
		if ( key.startsWith( prefix ) ) {

			String keyWithoutPrefix = key.substring( prefix.length() );

			Map<String, Object> keys = keyToMap( entityKeyMetadata, keyWithoutPrefix );

			for ( Map.Entry<String, Object> entry : keys.entrySet() ) {
				document.put( entry.getKey(), entry.getValue() );
			}
		}
	}
}
