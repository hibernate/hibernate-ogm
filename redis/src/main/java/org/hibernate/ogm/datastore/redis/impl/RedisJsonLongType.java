package org.hibernate.ogm.datastore.redis.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.WrappedGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;

/**
 * Type for storing {@code long}s in Redis JSON. The JSON parser uses {@link java.math.BigDecimal}s to deserialize integer data
 * types.
 *
 * @author Mark Paluch
 */
public class RedisJsonLongType extends AbstractGenericBasicType<Long> {

	public static final RedisJsonLongType INSTANCE = new RedisJsonLongType();

	public RedisJsonLongType() {
		super( WrappedGridTypeDescriptor.INSTANCE, LongTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "redis_long";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}