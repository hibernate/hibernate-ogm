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
import org.hibernate.type.descriptor.java.ByteArrayTypeDescriptor;

/**
 * Persist an array of {@link Byte} as {@link String}.
 *
 * @see StringMappedGridTypeDescriptor
 * @author Davide D'Alto
 */
public class ByteArrayStringType extends AbstractGenericBasicType<Byte[]> {

	public static final ByteArrayStringType INSTANCE = new ByteArrayStringType();

	public ByteArrayStringType() {
		super( StringMappedGridTypeDescriptor.INSTANCE, ByteArrayTypeDescriptor.INSTANCE );
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
