/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;

/**
 * @author Mark Paluch
 */
public class RedisJsonBlobType extends AbstractGenericBasicType<byte[]> {

	public static final RedisJsonBlobType INSTANCE = new RedisJsonBlobType();

	public RedisJsonBlobType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, Base64ByteArrayTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "redis_byte_array";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
