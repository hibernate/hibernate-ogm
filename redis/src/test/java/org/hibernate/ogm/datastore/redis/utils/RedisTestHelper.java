/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.redis.AbstractRedisDialect;
import org.hibernate.ogm.datastore.redis.Redis;
import org.hibernate.ogm.datastore.redis.RedisHashDialect;
import org.hibernate.ogm.datastore.redis.RedisJsonDialect;
import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.ogm.utils.TestableGridDialect;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambdaworks.redis.api.sync.RedisCommands;

public class RedisTestHelper implements TestableGridDialect {

	static {
		// Read host and port from environment variable
		// Maven's surefire plugin set it to the string 'null'
		String redisHostName = System.getenv( "REDIS_HOSTNAME" );
		if ( isNotNull( redisHostName ) ) {
			System.getProperties().setProperty( OgmProperties.HOST, redisHostName );
		}
		String redisPort = System.getenv( "REDIS_PORT" );
		if ( isNotNull( redisPort ) ) {
			System.getProperties().setProperty( OgmProperties.PORT, redisPort );
		}
	}

	private static boolean isNotNull(String value) {
		return value != null && value.length() > 0 && !"null".equals( value );
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		RedisDatastoreProvider castProvider = getProvider( sessionFactory );
		AbstractRedisDialect gridDialect = getGridDialect( castProvider );

		if ( gridDialect instanceof RedisJsonDialect ) {
			return extractFromJsonDialect( key, (RedisJsonDialect) gridDialect );
		}

		if ( gridDialect instanceof RedisHashDialect ) {
			return extractFromHashDialect( sessionFactory, key, (RedisHashDialect) gridDialect );
		}

		throw new IllegalStateException( "Unsupported dialect " + gridDialect );
	}


	private Map<String, Object> extractFromJsonDialect(
			EntityKey key,
			RedisJsonDialect gridDialect
	) {

		Entity entity = gridDialect.getEntityStorageStrategy().getEntity(
				gridDialect.entityId( key )
		);

		if ( entity == null ) {
			return null;
		}

		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			entity.set( key.getColumnNames()[i], key.getColumnValues()[i] );
		}
		return new HashMap<>( entity.getProperties() );
	}

	private Map<String, Object> extractFromHashDialect(
			SessionFactory sessionFactory,
			EntityKey key,
			RedisHashDialect gridDialect) {

		RedisCommands<String, String> connection = getConnection( sessionFactory );

		String entityId = gridDialect.entityId( key );

		if ( !connection.exists( entityId ) ) {
			return null;
		}

		Map<String, String> hgetall = connection.hgetall( entityId );

		Map<String, Object> result = new HashMap<>();
		result.putAll( hgetall );

		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			result.put( key.getColumnNames()[i], key.getColumnValues()[i] );
		}

		return result;
	}

	private static RedisDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService(
				DatastoreProvider.class
		);
		if ( !( RedisDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with RedisDatastoreProvider, cannot extract underlying map." );
		}

		return RedisDatastoreProvider.class.cast( provider );
	}

	@Override
	public AbstractRedisDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return getDialect( (RedisDatastoreProvider) datastoreProvider );
	}

	public static AbstractRedisDialect getDialect(RedisDatastoreProvider datastoreProvider) {
		if ( TestHelper.getCurrentDialectType() == GridDialectType.REDIS_HASH ) {
			return new RedisHashDialect( datastoreProvider );
		}
		return new RedisJsonDialect( datastoreProvider );
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		RedisCommands<String, String> connection = getConnection( sessionFactory );
		connection.flushall();
	}

	private RedisCommands<String, String> getConnection(SessionFactory sessionFactory) {
		RedisDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getConnection();
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		return null;
	}

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		RedisCommands<String, String> connection = getConnection( sessionFactory );
		List<String> keys = connection.keys( "*" );

		long result = 0;
		for ( String key : keys ) {
			if ( key.startsWith( AbstractRedisDialect.ASSOCIATIONS ) || key.startsWith( AbstractRedisDialect.IDENTIFIERS ) ) {
				continue;
			}

			String type = connection.type( key );

			if ( type.equals( "hash" ) || type.equals( "string" ) ) {
				result++;
			}
		}

		return result;
	}

	public static void assertDbObject(
			OgmSessionFactory sessionFactory,
			String hash,
			String key,
			String expectedDbObject) {
		RedisDatastoreProvider provider = getProvider( sessionFactory );
		try {

			String actual = provider.getConnection().get( hash + ":" + key );
			JSONAssert.assertEquals( expectedDbObject, actual, JSONCompareMode.LENIENT );
		}
		catch (JSONException e) {
			throw new IllegalStateException( e );
		}
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		long associationCount = getNumberOfGlobalAssociations( sessionFactory );
		associationCount += getNumberOfEmbeddedAssociations( sessionFactory );

		return associationCount;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		switch ( type ) {
			case ASSOCIATION_DOCUMENT:
				return getNumberOfGlobalAssociations( sessionFactory );
			case IN_ENTITY:
				return getNumberOfEmbeddedAssociations( sessionFactory );
			default:
				throw new IllegalArgumentException( "Unexpected association storaget type " + type );
		}
	}

	public long getNumberOfGlobalAssociations(SessionFactory sessionFactory) {
		RedisCommands<String, String> connection = getConnection( sessionFactory );
		return connection.keys( AbstractRedisDialect.ASSOCIATIONS + ":*" ).size();
	}

	public long getNumberOfEmbeddedAssociations(SessionFactory sessionFactory) {
		RedisCommands<String, String> connection = getConnection( sessionFactory );

		long associationCount = 0;
		List<String> keys = connection.keys( "*" );

		for ( String key : keys ) {

			if ( key.startsWith( AbstractRedisDialect.ASSOCIATIONS ) || key.startsWith( AbstractRedisDialect.IDENTIFIERS ) ) {
				continue;
			}

			String type = connection.type( key );

			if ( "string".equalsIgnoreCase( type ) ) {
				String value = connection.get( key );
				JsonNode jsonNode = fromJSON( value );

				for ( JsonNode node : jsonNode ) {
					if ( node.isArray() ) {
						associationCount++;
					}
				}
			}
		}

		return associationCount;
	}

	public static JsonNode fromJSON(String json) {
		if ( json == null || json.length() == 0 ) {
			return null;
		}

		try {
			ObjectMapper objectMapper = new ObjectMapper().configure( JsonParser.Feature.ALLOW_SINGLE_QUOTES, true );
			return objectMapper.reader().readTree( json );
		}
		catch (IOException e) {
			throw new IllegalStateException( e );
		}
	}

	@Override
	public Class<? extends DatastoreConfiguration<?>> getDatastoreConfigurationType() {
		return Redis.class;
	}
}
