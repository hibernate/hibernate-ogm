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
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.AssociationType;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.TupleOperation;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.Type;

import com.lambdaworks.redis.KeyScanCursor;
import com.lambdaworks.redis.RedisConnection;
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
 */
public class RedisJsonDialect extends AbstractRedisDialect implements MultigetGridDialect {

	protected final RedisConnection<String, String> connection;

	protected final JsonEntityStorageStrategy entityStorageStrategy;

	public RedisJsonDialect(RedisDatastoreProvider provider) {
		super( provider.getConnection() );
		connection = provider.getConnection();
		this.entityStorageStrategy = new JsonEntityStorageStrategy( strategy, connection );
	}

	@Override
	public GridType overrideType(Type type) {
		return strategy.overrideType( type );
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
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		Map<String, Object> map = ( (RedisTupleSnapshot) tuple.getSnapshot() ).getMap();
		MapHelpers.applyTupleOpsOnMap( tuple, map );
		storeEntity( key, map, tupleContext.getOptionsContext(), tuple.getOperations() );
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
			storeAssociation( associationKey, (Association) redisAssociation.getOwningDocument() );
			setAssociationTTL( associationKey, associationContext, currentTtl );
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
			removeAssociation( key );
		}
	}

	@Override
	public void forEachTuple(final ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
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
					Entity document = entityStorageStrategy.getEntity( key );

					addKeyValuesFromKeyName( entityKeyMetadata, prefix, key, document );

					consumer.consume( new Tuple( new RedisTupleSnapshot( document.getProperties() ) ) );
				}

			} while ( !cursor.isFinished() );
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

	public JsonEntityStorageStrategy getEntityStorageStrategy() {
		return entityStorageStrategy;
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
		List<Tuple> tuples = new ArrayList<Tuple>( keys.length );

		int i = 0;
		for ( Entity entity : entities ) {
			if ( entity != null ) {
				EntityKey key = keys[i];
				addIdToEntity( entity, key.getColumnNames(), key.getColumnValues() );
				tuples.add( new Tuple( new RedisTupleSnapshot( entity.getProperties() ) ) );
			}
			else {
				tuples.add( null );
			}
			i++;
		}

		return tuples;
	}
}
