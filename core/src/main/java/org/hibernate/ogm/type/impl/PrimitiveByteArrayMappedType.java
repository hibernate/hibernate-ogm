/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.ByteArrayMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayTypeDescriptor;

/**
 * Persist a value by wrapping and unwrapping in an array of bytes.
 *
 * @author Davide D'Alto
 */
public class PrimitiveByteArrayMappedType extends AbstractGenericBasicType<byte[]> {
	public static final PrimitiveByteArrayMappedType INSTANCE = new PrimitiveByteArrayMappedType();

	public PrimitiveByteArrayMappedType() {
		super( ByteArrayMappedGridTypeDescriptor.INSTANCE, PrimitiveByteArrayTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "byte_array";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), int.class.getName(), Byte.class.getName() };
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
