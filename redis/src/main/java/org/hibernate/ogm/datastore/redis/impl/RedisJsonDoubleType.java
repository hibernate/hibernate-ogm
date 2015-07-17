package org.hibernate.ogm.datastore.redis.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.WrappedGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.DoubleTypeDescriptor;

/**
 * Type for storing {@code long}s in Redis.
 *
 * @author Mark Paluch
 */
public class RedisJsonDoubleType extends AbstractGenericBasicType<Double> {

	public static final RedisJsonDoubleType INSTANCE = new RedisJsonDoubleType();

	public RedisJsonDoubleType() {
		super( WrappedGridTypeDescriptor.INSTANCE, DoubleTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "redis_double";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}