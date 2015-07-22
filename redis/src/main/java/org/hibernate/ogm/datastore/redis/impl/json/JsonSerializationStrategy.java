/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl.json;

import java.io.IOException;
import java.util.Arrays;

import org.hibernate.ogm.datastore.redis.impl.SerializationStrategy;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.Type;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Serialize payload as JSON using the Jackson Mapper.
 *
 * @author Mark Paluch
 */
public class JsonSerializationStrategy implements SerializationStrategy {

	private static final byte[] NULL = "null".getBytes();
	private final ObjectMapper objectMapper;

	public JsonSerializationStrategy() {
		this.objectMapper = new ObjectMapper().configure( JsonParser.Feature.ALLOW_SINGLE_QUOTES, true );
	}

	@Override
	public <T> T deserialize(byte[] serialized, Class<T> targetType) {

		if ( serialized == null || serialized.length == 0 || Arrays.equals( serialized, NULL ) ) {
			return null;
		}

		try {
			JsonParser parser = objectMapper.reader().getFactory().createParser( serialized );
			return objectMapper.reader().readValue( parser, targetType );
		}
		catch (IOException e) {
			throw new IllegalStateException( e );
		}
	}

	@Override
	public byte[] serialize(Object payload) {
		try {
			return objectMapper.writer().writeValueAsBytes( payload );
		}
		catch (JsonProcessingException e) {
			throw new IllegalStateException( e );
		}
	}

	@Override
	public GridType overrideType(Type type) {
		return RedisJsonTypeConverter.INSTANCE.convert( type );
	}
}
