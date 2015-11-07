/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl.hash;

import java.util.UUID;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.NumericBooleanType;
import org.hibernate.type.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.java.ByteTypeDescriptor;
import org.hibernate.type.descriptor.java.DoubleTypeDescriptor;
import org.hibernate.type.descriptor.java.FloatTypeDescriptor;
import org.hibernate.type.descriptor.java.IntegerTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.LongTypeDescriptor;
import org.hibernate.type.descriptor.java.ShortTypeDescriptor;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;

/**
 * @author Mark Paluch
 */
public class RedisHashType<T> extends AbstractGenericBasicType<T> {

	public static final RedisHashType<Long> LONG = new RedisHashType<>( LongTypeDescriptor.INSTANCE, "long" );
	public static final RedisHashType<Integer> INTEGER = new RedisHashType<>(
			IntegerTypeDescriptor.INSTANCE,
			"integer"
	);
	public static final RedisHashType<Short> SHORT = new RedisHashType<>( ShortTypeDescriptor.INSTANCE, "short" );
	public static final RedisHashType<Float> FLOAT = new RedisHashType<>( FloatTypeDescriptor.INSTANCE, "float" );
	public static final RedisHashType<Double> DOUBLE = new RedisHashType<>( DoubleTypeDescriptor.INSTANCE, "double" );
	public static final RedisHashType<Boolean> NUMERIC_BOOLEAN = new RedisHashType<>(
			NumericBooleanType.INSTANCE.getJavaTypeDescriptor(),
			"numeric_boolean"
	);
	public static final RedisHashType<Boolean> BOOLEAN = new RedisHashType<>(
			BooleanTypeDescriptor.INSTANCE,
			"boolean"
	);
	public static final RedisHashType<UUID> UUID_BINARY = new RedisHashType<>(
			UUIDTypeDescriptor.INSTANCE,
			"uuid_binary"
	);
	public static final RedisHashType<Byte> BYTE = new RedisHashType<>( ByteTypeDescriptor.INSTANCE, "byte" );

	private final String name;

	public RedisHashType(JavaTypeDescriptor<T> javaTypeDescriptor, String name) {
		super( StringMappedGridTypeDescriptor.INSTANCE, javaTypeDescriptor );
		this.name = "redis_hash_" + name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
