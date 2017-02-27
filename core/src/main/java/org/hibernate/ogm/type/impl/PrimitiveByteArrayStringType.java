/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.type.descriptor.impl.StringMappedGridTypeDescriptor;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayTypeDescriptor;

/**
 * Persist a {@link byte[]} as a {@link String}.
 *
 * @author Davide D'Alto
 */
public class PrimitiveByteArrayStringType extends AbstractGenericBasicType<byte[]>  {

	public static final PrimitiveByteArrayStringType INSTANCE = new PrimitiveByteArrayStringType();

	public PrimitiveByteArrayStringType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, PrimitiveByteArrayTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "byte_array_as_string";
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}
}
