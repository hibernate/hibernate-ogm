/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import java.io.Serializable;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * A {@link GridType} which stores/retrieves values from the grid unwrapping/wrapping them as string
 * representation of the correpsonding byte array using the chosend {@link JavaTypeDescriptor}
 *
 * @see Base64ByteArrayTypeDescriptor
 * @author Davide D'Alto
 */
public class RedisSerializableType<T extends Serializable> extends AbstractGenericBasicType<T> {

	public RedisSerializableType(JavaTypeDescriptor<T> javaTypeDescriptor) {
		super( Base64ByteArrayMappedGridTypeDescriptor.INSTANCE, javaTypeDescriptor );
	}

	@Override
	public String getName() {
		return "redis_serializable";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
