/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl.hash;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.datastore.redis.impl.EntityStorageStrategy;
import org.hibernate.ogm.datastore.redis.impl.json.RedisJsonTypeConverter;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.ogm.util.Experimental;
import org.hibernate.type.BinaryType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.protocol.LettuceCharsets;

/**
 * Map entities to hashes in Redis using {@literal HGETALL} and {@literal HMSET} commands.
 * The value represents the entity as String. This strategy does not support embedded associations and nested objects.
 *
 * @author Mark Paluch
 */
@Experimental("Experimental strategy. Embedded associations and lists are not supported")
public class ExperimentalHashEntityStorageStrategy implements EntityStorageStrategy {

	private static final Map<Class<?>, Type> conversionMap = createTypeConversionMap();
	private final RedisConnection<byte[], byte[]> connection;

	public ExperimentalHashEntityStorageStrategy(
			RedisConnection<byte[], byte[]> connection) {
		this.connection = connection;
	}

	@Override
	public Entity getEntity(byte[] key) {

		Map<byte[], byte[]> map = connection.hgetall( key );

		if ( map == null || map.isEmpty() ) {
			return null;
		}

		Entity result = new Entity();
		for ( Map.Entry<byte[], byte[]> entry : map.entrySet() ) {
			String propertyKey = new String( entry.getKey(), LettuceCharsets.UTF8 );
			String propertyValue = new String( entry.getValue(), LettuceCharsets.UTF8 );
			result.set( propertyKey, propertyValue );
		}

		return result;
	}

	@Override
	public void storeEntity(byte[] key, Entity entity) {

		Map<byte[], byte[]> map = new HashMap<>();

		for ( Map.Entry<String, Object> entry : entity.getProperties().entrySet() ) {

			byte[] propertyKey = entry.getKey().getBytes( LettuceCharsets.UTF8 );
			byte[] propertyValue = getPropertyValue( entry.getValue() );
			if ( propertyValue == null ) {
				throw new UnsupportedOperationException(
						"Cannot store value '" + entry.getValue() + "' for key '" + entry.getKey() + "' to Redis, Data type " + entry
								.getValue()
								.getClass()
								.getName() + " is not supported with hash storage"
				);
			}

			map.put( propertyKey, propertyValue );
		}

		connection.hmset( key, map );
	}

	private byte[] getPropertyValue(Object value) {

		if ( value == null ) {
			return new byte[0];
		}

		if ( value instanceof String ) {
			return ( (String) value ).getBytes( LettuceCharsets.UTF8 );
		}

		if ( value instanceof byte[] ) {
			return (byte[]) value;
		}

		Type type = conversionMap.get( value.getClass() );
		if ( type != null ) {
			AbstractGenericBasicType<Object> gridType = RedisJsonTypeConverter.INSTANCE.convert( type );
			if ( gridType != null ) {
				return gridType.toString( value ).getBytes( LettuceCharsets.UTF8 );
			}
		}

		return null;
	}


	private static Map<Class<?>, Type> createTypeConversionMap() {
		Map<Class<?>, Type> conversion = new HashMap<Class<?>, Type>();
		conversion.put( Calendar.class, StandardBasicTypes.CALENDAR );
		conversion.put( Date.class, StandardBasicTypes.DATE );
		conversion.put( Time.class, StandardBasicTypes.TIME );
		conversion.put( Timestamp.class, StandardBasicTypes.TIMESTAMP );
		conversion.put( byte[].class, BinaryType.INSTANCE );
		conversion.put( Byte[].class, BinaryType.INSTANCE );
		conversion.put( byte.class, StandardBasicTypes.BYTE );
		conversion.put( Byte.class, StandardBasicTypes.BYTE );
		conversion.put( int.class, StandardBasicTypes.INTEGER );
		conversion.put( Integer.class, StandardBasicTypes.INTEGER );
		conversion.put( long.class, StandardBasicTypes.LONG );
		conversion.put( Long.class, StandardBasicTypes.LONG );
		conversion.put( double.class, StandardBasicTypes.DOUBLE );
		conversion.put( Double.class, StandardBasicTypes.DOUBLE );
		return conversion;
	}
}
