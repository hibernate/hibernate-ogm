/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.WrappedGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.type.descriptor.java.ByteTypeDescriptor;

/**
 * Type for storing {@code byte}s in Redis as JSON. They are stored as JSON numbers.
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 * @author Mark Paluch
 */
public class RedisJsonByteType extends AbstractGenericBasicType<Byte> {

	public static final RedisJsonByteType INSTANCE = new RedisJsonByteType();

	public RedisJsonByteType() {
		super( WrappedGridTypeDescriptor.INSTANCE, ByteTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "redis_byte";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
