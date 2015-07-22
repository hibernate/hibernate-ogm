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
