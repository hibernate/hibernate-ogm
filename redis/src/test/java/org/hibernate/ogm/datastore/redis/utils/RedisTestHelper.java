/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.redis.Redis;
import org.hibernate.ogm.datastore.redis.RedisDialect;
import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.redis.options.EntityStorageType;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.utils.TestableGridDialect;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambdaworks.redis.RedisConnection;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

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
		RedisDialect gridDialect = getGridDialect( castProvider );
		Entity entity = gridDialect.getEntityStorageStrategy( EntityStorageType.JSON ).getEntity(
				gridDialect.entityId(
						key
				)
		);

		if ( entity != null ) {
			for ( int i = 0; i < key.getColumnNames().length; i++ ) {
				entity.set( key.getColumnNames()[i], key.getColumnValues()[i] );
			}
		}

		return entity.getProperties();
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
	public RedisDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return getDialect( datastoreProvider );
	}

	public static RedisDialect getDialect(DatastoreProvider datastoreProvider) {
		return new RedisDialect( (RedisDatastoreProvider) datastoreProvider );
	}

	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		RedisConnection<byte[], byte[]> connection = getConnection( sessionFactory );
		connection.flushall();
	}

	private RedisConnection<byte[], byte[]> getConnection(SessionFactory sessionFactory) {
		RedisDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getConnection();
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		return null;
	}

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		RedisConnection<byte[], byte[]> connection = getConnection( sessionFactory );
		List<byte[]> keys = connection.keys( RedisDialect.toBytes( "*" ) );

		long result = 0;
		for ( byte[] key : keys ) {
			String keyAsString = RedisDialect.toString( key );
			if ( keyAsString.startsWith( RedisDialect.ASSOCIATIONS ) || keyAsString.startsWith( RedisDialect.IDENTIFIERS ) ) {
				continue;
			}

			String type = connection.type( key );

			if ( type.equals( "hash" ) ) {
				result += connection.hlen( key );
			}
			else {
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

			byte[] actual = provider.getConnection().get( RedisDialect.toBytes( hash + ":" + key ) );
			JSONAssert.assertEquals( expectedDbObject, RedisDialect.toString( actual ), JSONCompareMode.LENIENT );
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

		RedisConnection<byte[], byte[]> connection = getConnection( sessionFactory );
		return connection.keys( RedisDialect.toBytes( RedisDialect.ASSOCIATIONS + ":*" ) ).size();
	}

	public long getNumberOfEmbeddedAssociations(SessionFactory sessionFactory) {
		RedisConnection<byte[], byte[]> connection = getConnection( sessionFactory );

		long associationCount = 0;
		List<byte[]> keys = connection.keys( RedisDialect.toBytes( "*" ) );

		for ( byte[] key : keys ) {

			String keyAsString = RedisDialect.toString( key );
			if ( keyAsString.startsWith( RedisDialect.ASSOCIATIONS ) || keyAsString.startsWith( RedisDialect.IDENTIFIERS ) ) {
				continue;
			}

			String type = connection.type( key );

			if ( "string".equalsIgnoreCase( type ) ) {
				byte[] value = connection.get( key );
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


	@Override
	public GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration) {
		return configuration.configureOptionsFor( Redis.class );
	}

	public static JsonNode fromJSON(byte[] json) {
		if ( json == null || json.length == 0 ) {
			return null;
		}

		try {
			ObjectMapper objectMapper = new ObjectMapper().configure( JsonParser.Feature.ALLOW_SINGLE_QUOTES, true );
			return objectMapper.reader().readTree( new ByteArrayInputStream( json ) );
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
